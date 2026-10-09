const registry = require('../../features');
const petService = require('../../services/pet');
const privacy = require('../../utils/privacy');
const config = require('../../config');
const mediaService = require('../../services/media');
const feedingService = require('../../services/feeding');
const userService = require('../../services/user');

Component({
  properties: {
    pageKey: { type: String, value: '' },
    options: { type: Object, value: {} }
  },
  data: { title: '', description: '', summary: '', rows: [], fields: [], form: {}, actions: [], loading: true, saving: false, error: '', photoPaths: ['', '', ''], photoSlots: [0, 1, 2], uploadPaths: [], page: 0, total: 0, hasMore: false },
  observers: {
    pageKey(value) { if (value) this.initialize(); },
    options(value) { if (this.properties.pageKey && value) this.initialize(); }
  },
  lifetimes: { ready() { this.initialize(); } },
  methods: {
    initialize(force) {
      const key = this.properties.pageKey;
      const options = this.properties.options || {};
      const signature = `${key}:${options.id || ''}:${options.petId || ''}`;
      if (!key || (!force && (this._signature === signature || this._loading))) return;
      this._signature = signature;
      this._loading = true;
      const definition = registry[key];
      if (!definition) {
        this.setData({ title: '页面未配置', description: '请返回后重试', loading: false, error: '' });
        this._loading = false;
        return;
      }
      const fields = (definition.fields || []).map((field) => Object.assign({}, field, {
        optionLabels: (field.options || []).map((option) => option.label)
      }));
      const form = {};
      fields.forEach((field) => { form[field.key] = field.defaultValue === undefined ? '' : field.defaultValue; });
      this.setData({ title: definition.title, description: definition.description || '', summary: '', rows: [], fields: this.decorateFields(fields, form), form, actions: definition.actions || [], loading: true, saving: false, error: '', photoPaths: ['', '', ''], uploadPaths: [], page: 0, total: 0, hasMore: false });
      wx.setNavigationBarTitle({ title: definition.title });
      this.resolvePet(options, definition).then((pet) => {
        if (definition.requiresPet && !pet) {
          this.setData({ loading: false, error: '请先添加宠物档案，再使用此功能。', actions: [{ id: 'add-pet', label: '添加宠物', route: config.routes.petCreate }] });
          return null;
        }
        if (!definition.load) { this.setData({ loading: false }); return null; }
        return definition.load(this.context(pet, form)).then((result) => {
          const next = result || {};
          const nextFields = next.fields ? next.fields.map((field) => Object.assign({}, field, { optionLabels: (field.options || []).map((option) => option.label) })) : this.data.fields;
          const nextForm = {};
          nextFields.forEach((field) => { nextForm[field.key] = field.defaultValue === undefined ? '' : field.defaultValue; });
          const finalForm = Object.assign(nextForm, next.form || {});
          this.setData({
            summary: next.summary || '',
            rows: next.rows || [],
            title: next.title || this.data.title,
            actions: next.actions || definition.actions || [],
            fields: this.decorateFields(nextFields, finalForm),
            form: finalForm,
            page: next.page || 0,
            total: next.total || 0,
            hasMore: !!next.hasMore,
            loading: false,
            error: next.error || ''
          });
          if (next.title) wx.setNavigationBarTitle({ title: next.title });
        });
      }).catch((error) => this.setData({ loading: false, error: error.message || '资料加载失败，请重试' }))
        .finally(() => { this._loading = false; });
    },

    resolvePet(options, definition) {
      const current = getApp().globalData.currentPet;
      const requestedId = options.petId || (definition.petIdFromId ? options.id : '');
      if (!requestedId || String(requestedId) === String(current && current.id)) return Promise.resolve(current);
      return petService.get(requestedId).then((pet) => { getApp().setCurrentPet(pet); return pet; });
    },

    context(pet, form) { return { pet, form, options: this.properties.options || {}, page: this }; },

    decorateFields(fields, form) {
      return fields.map((field) => {
        const selected = (field.options || []).find((option) => String(option.value) === String(form[field.key]));
        return Object.assign({}, field, { displayValue: selected ? selected.label : '' });
      });
    },

    reload() { if (!this._loading) this.initialize(true); },

    onInput(event) {
      const key = event.currentTarget.dataset.key;
      this.setData({ [`form.${key}`]: event.detail.value });
    },

    onSelect(event) {
      const key = event.currentTarget.dataset.key;
      const field = this.data.fields.find((item) => item.key === key);
      if (!field) return;
      const option = field.options[Number(event.detail.value)];
      if (!option) return;
      const form = Object.assign({}, this.data.form, { [key]: option.value });
      this.setData({ form, fields: this.decorateFields(this.data.fields, form) });
    },

    onChoice(event) {
      const key = event.currentTarget.dataset.key;
      const value = event.currentTarget.dataset.value;
      const form = Object.assign({}, this.data.form, { [key]: value });
      this.setData({ form, fields: this.decorateFields(this.data.fields, form) });
    },

    onChoosePhoto(event) {
      const index = Number(event.currentTarget.dataset.index);
      privacy.authorizePrivacy().then(() => new Promise((resolve, reject) => {
        wx.chooseMedia({ count: 1, mediaType: ['image'], sourceType: ['album', 'camera'], success: resolve, fail: reject });
      })).then((result) => {
        const photo = result.tempFiles && result.tempFiles[0];
        if (!photo) return;
        const setPath = (path) => wx.getFileInfo({
          filePath: path,
          success: (info) => {
            if (info.size > config.uploads.petPhotoMaxBytes) {
              this.setData({ error: '压缩后的照片仍较大，请选择较小的图片（每张不超过 2.5 MB）' });
              return;
            }
            const paths = this.data.photoPaths.slice();
            paths[index] = path;
            this.setData({ photoPaths: paths, error: '' });
          },
          fail: () => this.setData({ error: '无法读取图片，请重新选择' })
        });
        wx.compressImage({ src: photo.tempFilePath, quality: 82, success: (compressed) => setPath(compressed.tempFilePath), fail: () => setPath(photo.tempFilePath) });
      }).catch(() => {});
    },

    selectUploads() {
      if (this.data.saving) return;
      privacy.authorizePrivacy().then(() => new Promise((resolve, reject) => {
        wx.chooseMedia({ count: config.uploads.photoLimit, mediaType: ['image', 'video'], sourceType: ['album', 'camera'], success: resolve, fail: reject });
      })).then((result) => {
        const files = (result.tempFiles || []).slice(0, config.uploads.photoLimit).map((file, index) => ({
          path: file.tempFilePath,
          name: file.tempFilePath.split('/').pop() || `media-${Date.now()}-${index}`,
          type: file.fileType === 'video' ? 'video/mp4' : 'image/jpeg',
          size: file.size
        }));
        const tooLarge = files.find((file) => file.size > (file.type.indexOf('video') === 0 ? config.uploads.videoMaxBytes : config.uploads.imageMaxBytes));
        if (tooLarge) { this.setData({ error: '所选文件超出单个文件大小限制，请先压缩后重试' }); return; }
        this._uploadFiles = files;
        this.setData({ uploadPaths: files.map((file) => file.path), error: '' });
      }).catch(() => {});
    },

    uploadSelectedMedia() {
      if (this.data.saving) return;
      const files = this._uploadFiles || [];
      if (!files.length) { this.setData({ error: '请先选择照片或视频' }); return; }
      const definition = registry[this.properties.pageKey];
      this.setData({ saving: true, error: '' });
      definition.submit(this.context(getApp().globalData.currentPet, this.data.form), files)
        .then((result) => { this._uploadFiles = []; this.setData({ summary: `已上传 ${Array.isArray(result) ? result.length : files.length} 项素材，正在整理相册。`, uploadPaths: [], rows: [] }); })
        .catch((error) => {
          const completed = error.uploadedCount || 0;
          this._uploadFiles = files.slice(completed);
          this.setData({ uploadPaths: this._uploadFiles.map((file) => file.path), error: error.message || '上传失败，请重试' });
        })
        .finally(() => this.setData({ saving: false }));
    },

    recognizeFood() {
      if (this.data.saving) return;
      const pet = getApp().globalData.currentPet;
      if (this.properties.pageKey === 'food-library') {
        wx.navigateTo({ url: `${config.routes.foodRecognition}?petId=${pet && pet.id || ''}` });
        return;
      }
      if (!pet) { this.setData({ error: '请先添加宠物档案' }); return; }
      privacy.authorizePrivacy().then(() => new Promise((resolve, reject) => {
        wx.chooseMedia({ count: 1, mediaType: ['image'], sourceType: ['album', 'camera'], success: resolve, fail: reject });
      })).then((result) => {
        const file = result.tempFiles && result.tempFiles[0];
        if (!file) return null;
        if (file.size > config.uploads.imageMaxBytes) {
          this.setData({ error: '包装照片过大，请选择 10 MB 以内的图片' });
          return null;
        }
        this.setData({ saving: true, error: '' });
        return mediaService.photos.upload([{ path: file.tempFilePath, name: 'food-package.jpg', type: 'image/jpeg' }], { petId: pet.id })
          .then((uploads) => feedingService.recognition.create({ petId: pet.id, mediaId: uploads[0].id, idempotencyKey: `food-${Date.now()}` }));
      }).then((task) => {
        if (!task) return;
        const draft = task.draft || {};
        const recognitionId = task.taskId;
        const currentForm = this.data.form;
        const recognizedFields = {
          recognitionId,
          name: draft.name || draft.productName || draft.foodName || draft.product_name || currentForm.name || '',
          brand: draft.brand || currentForm.brand || '',
          foodType: draft.foodType || draft.food_type || currentForm.foodType || '',
          specification: draft.specification || currentForm.specification || ''
        };
        const warnings = Array.isArray(task.warnings) && task.warnings.length ? `\n识别提示：${task.warnings.join('；')}` : '';
        const isPending = task.status === 'pending' || task.status === 'processing';
        this.setData({
          summary: `${isPending ? '识别任务已提交，稍后刷新结果。' : '请核对识别草稿和包装信息后，再保存到食品档案。'}${warnings}`,
          form: Object.assign({}, currentForm, recognizedFields),
          actions: isPending
            ? [{ id: 'refresh-recognition', label: '刷新识别结果', primary: true }]
            : ['failed', 'expired', 'rejected'].includes(task.status)
              ? [{ id: 'recognize-food', label: '重新识别包装', primary: true }]
              : [],
          rows: [{ id: recognitionId || 'recognition', title: recognizedFields.name || (isPending ? '食品识别中' : '识别结果已返回'), subtitle: task.errorMessage || `识别状态：${task.status || '未知'}。请核对包装信息。`, meta: recognitionId ? `任务编号 ${recognitionId}` : '', chevron: false }]
        });
      }).catch((error) => this.setData({ error: error.message || '无法提交识别任务' }))
        .finally(() => this.setData({ saving: false }));
    },

    onAction(event) {
      const action = event.currentTarget.dataset.action;
      this.runAction(action);
    },

    runAction(id) {
      if (id === 'refresh-recognition') { this.refreshFoodRecognition(); return; }
      const definition = registry[this.properties.pageKey];
      const action = (this.data.actions || []).find((item) => item.id === id) || (definition.actions || []).find((item) => item.id === id);
      if (!action) return;
      if (action.route) { wx.navigateTo({ url: this.interpolateRoute(action.route) }); return; }
      if (action.tab) { wx.switchTab({ url: action.tab }); return; }
      if (action.id === 'change-avatar') { this.changeAvatar(); return; }
      if (action.id === 'select-media') { this.selectUploads(); return; }
      if (action.id === 'upload-media') { this.uploadSelectedMedia(); return; }
      if (action.id === 'recognize-food') { this.recognizeFood(); return; }
      if (action.run) {
        const context = this.context(getApp().globalData.currentPet, this.data.form);
        Promise.resolve(action.run(context, this.data))
          .then((result) => action.afterRun ? action.afterRun(context, result) : this.initialize(true))
          .catch((error) => this.setData({ error: error.message || '操作失败' }));
      }
    },

    refreshFoodRecognition() {
      const recognitionId = this.data.form.recognitionId;
      if (!recognitionId || this.data.saving) return;
      this.setData({ saving: true, error: '' });
      feedingService.recognition.get(recognitionId).then((task) => {
        const draft = task.draft || {};
        const form = Object.assign({}, this.data.form, {
          name: draft.name || draft.productName || draft.foodName || draft.product_name || this.data.form.name || '',
          brand: draft.brand || this.data.form.brand || '',
          foodType: draft.foodType || draft.food_type || this.data.form.foodType || '',
          specification: draft.specification || this.data.form.specification || ''
        });
        const isPending = task.status === 'pending' || task.status === 'processing';
        const warningText = Array.isArray(task.warnings) && task.warnings.length ? `\n识别提示：${task.warnings.join('；')}` : '';
        this.setData({
          form,
          summary: `${isPending ? '识别仍在处理中，请稍后刷新。' : '请核对识别草稿和包装信息后，再保存到食品档案。'}${warningText}`,
          actions: isPending
            ? [{ id: 'refresh-recognition', label: '刷新识别结果', primary: true }]
            : ['failed', 'expired', 'rejected'].includes(task.status)
              ? [{ id: 'recognize-food', label: '重新识别包装', primary: true }]
              : [],
          rows: [{ id: task.taskId || recognitionId, title: form.name || (isPending ? '食品识别中' : '识别结果已返回'), subtitle: task.errorMessage || `识别状态：${task.status || '未知'}。请核对包装信息。`, meta: `任务编号 ${task.taskId || recognitionId}`, chevron: false }]
        });
      }).catch((error) => this.setData({ error: error.message || '读取识别结果失败' }))
        .finally(() => this.setData({ saving: false }));
    },

    changeAvatar() {
      if (this.data.saving) return;
      this.setData({ saving: true, error: '' });
      privacy.authorizePrivacy().then(() => new Promise((resolve, reject) => {
        wx.chooseMedia({ count: 1, mediaType: ['image'], sourceType: ['album', 'camera'], success: resolve, fail: reject });
      })).then((result) => {
        const file = result.tempFiles && result.tempFiles[0];
        if (!file) return null;
        if (file.size > config.uploads.imageMaxBytes) throw { message: '头像图片请控制在 10 MB 以内' };
        return userService.uploadAvatar(file.tempFilePath);
      }).then((user) => {
        if (!user) return;
        wx.setStorageSync(config.storage.user, user);
        getApp().globalData.user = user;
        this.setData({ summary: '头像已更新' });
        wx.showToast({ title: '头像已更新', icon: 'success' });
      }).catch((error) => {
        if (error && error.errMsg && error.errMsg.indexOf('cancel') >= 0) return;
        this.setData({ error: error.message || '头像上传失败，请重试' });
      }).finally(() => this.setData({ saving: false }));
    },

    interpolateRoute(route) {
      return route.replace(':petId', this.properties.options.petId || getApp().globalData.currentPet && getApp().globalData.currentPet.id || '')
        .replace(':id', this.properties.options.id || '');
    },

    onRowTap(event) {
      const row = this.data.rows[Number(event.currentTarget.dataset.index)];
      const definition = registry[this.properties.pageKey];
      if (definition.onRowTap) definition.onRowTap(this.context(getApp().globalData.currentPet, this.data.form), row);
    },

    onRowAction(event) {
      if (event && event.stopPropagation) event.stopPropagation();
      const row = this.data.rows[Number(event.currentTarget.dataset.index)];
      const definition = registry[this.properties.pageKey];
      if (definition.onRowAction) Promise.resolve(definition.onRowAction(this.context(getApp().globalData.currentPet, this.data.form), row, this))
        .then(() => this.initialize(true))
        .catch((error) => this.setData({ error: error.message || '操作失败' }));
    },

    onLoadMore() {
      if (this.data.saving || !this.data.hasMore || this.properties.pageKey !== 'photo-search') return;
      const nextPage = this.data.page + 1;
      const pet = getApp().globalData.currentPet;
      this.setData({ saving: true, error: '' });
      mediaService.photos.search({ query: this.data.form.query || '', petId: pet && pet.id, page: nextPage, size: config.request.pageSize })
        .then((result) => {
          const records = Array.isArray(result && result.records) ? result.records : [];
          const rows = records.map((item, index) => ({
            id: item.id || `photo-${nextPage}-${index}`,
            title: item.petName || item.fileName || '照片',
            subtitle: item.aiDescription || (item.tags || []).join(' · '),
            meta: (item.takenAt || '').slice(0, 10),
            imageUrl: item.fileUrl,
            chevron: true,
            raw: item
          }));
          const page = result && result.page || nextPage;
          const total = result && result.total || this.data.total;
          this.setData({ rows: this.data.rows.concat(rows), page, total, hasMore: page * config.request.pageSize < total });
        })
        .catch((error) => this.setData({ error: error.message || '加载更多照片失败' }))
        .finally(() => this.setData({ saving: false }));
    },

    onSubmit() {
      const definition = registry[this.properties.pageKey];
      if (!definition.submit || this.data.saving) return;
      const missing = this.data.fields.find((field) => field.required && this.data.form[field.key] === '');
      if (missing) { this.setData({ error: `请填写${missing.label}` }); return; }
      if (definition.requiresPhotos && !this.properties.options.id && this.data.photoPaths.filter(Boolean).length !== 3) { this.setData({ error: '请完成头像、正面照和侧面照三张照片' }); return; }
      this.setData({ saving: true, error: '' });
      definition.submit(this.context(getApp().globalData.currentPet, this.data.form), this.data.photoPaths)
        .then((result) => {
          wx.showToast({ title: definition.successMessage || '已保存', icon: 'success' });
          if (definition.afterSubmit) definition.afterSubmit(result, this.context(getApp().globalData.currentPet, this.data.form));
          else if (result && (result.rows || result.summary || result.form)) this.setData({ rows: result.rows || [], summary: result.summary || '', form: Object.assign({}, this.data.form, result.form || {}) });
          else this.initialize(true);
        })
        .catch((error) => this.setData({ error: error.message || '保存失败，请重试' }))
        .finally(() => this.setData({ saving: false }));
    }
  }
});
