const config = require('../config');
const pets = require('../services/pet');
const tags = require('../services/tags');
const h = require('./helpers');

const petList = {
  title: '我的宠物', description: '每个小伙伴都有自己的成长档案。',
  actions: [{ id: 'add', label: '添加宠物', primary: true, route: config.routes.petCreate }],
  load: () => pets.list().then((items) => ({ rows: h.simpleRows(items, (p) => p.name, (p) => `${p.breed || '品种未填写'} · ${p.type === 'cat' ? '猫咪' : p.type === 'dog' ? '狗狗' : '伙伴'}`, (p) => p.vectorizationStatus === 'completed' ? '成长档案已建立' : '照片处理中') })),
  onRowTap: (ctx, row) => wx.navigateTo({ url: `${config.routes.petDetail}?id=${row.id}` })
};

const petCreate = {
  title: '添加宠物', description: '先认识一下它，之后就能一起记录日常。', requiresPhotos: true,
  petIdFromId: true,
  fields: [
    { key: 'name', label: '宠物名字', placeholder: '例如：团子', required: true },
    { key: 'type', label: '宠物类型', type: 'select', required: true, options: [h.option('猫咪', 'cat'), h.option('狗狗', 'dog'), h.option('其他', 'other')] },
    { key: 'breed', label: '品种', placeholder: '可以稍后补充' },
    { key: 'gender', label: '性别', type: 'select', required: true, options: [h.option('弟弟', 'male'), h.option('妹妹', 'female'), h.option('暂不确定', 'unknown')] },
    { key: 'birthDate', label: '出生日期', type: 'date', placeholder: '选择日期' },
    { key: 'isNeutered', label: '是否已绝育', type: 'select', required: true, options: [h.option('是', true), h.option('否', false)] },
    { key: 'adoptDate', label: '到家日期', type: 'date', placeholder: '选择日期' },
    { key: 'weight', label: '当前体重（kg）', type: 'number', placeholder: '选填' },
    { key: 'photos', label: '宠物三视图', type: 'photos' }
  ],
  load: (ctx) => {
    if (!ctx.options.id) return Promise.resolve(null);
    return pets.get(ctx.options.id).then((pet) => ({
      title: '编辑宠物',
      summary: '更新基本资料。若要更换三视图照片，请从档案照片入口操作。',
      fields: [
        { key: 'name', label: '宠物名字', placeholder: '例如：团子', required: true },
        { key: 'breed', label: '品种' },
        { key: 'gender', label: '性别', type: 'select', required: true, options: [h.option('弟弟', 'male'), h.option('妹妹', 'female'), h.option('暂不确定', 'unknown')] },
        { key: 'birthDate', label: '出生日期', type: 'date' },
        { key: 'isNeutered', label: '是否已绝育', type: 'select', required: true, options: [h.option('是', true), h.option('否', false)] },
        { key: 'adoptDate', label: '到家日期', type: 'date' }
      ],
      form: { name: pet.name || '', breed: pet.breed || '', gender: pet.gender || 'unknown', birthDate: pet.birthDate || '', isNeutered: !!pet.isNeutered, adoptDate: pet.adoptDate || '' }
    }));
  },
  submit: (ctx, photoPaths) => ctx.options.id
    ? pets.update(ctx.options.id, Object.assign({}, ctx.form, { birthDate: ctx.form.birthDate || null, adoptDate: ctx.form.adoptDate || null }))
    : pets.create(ctx.form, photoPaths),
  afterSubmit: (pet) => { getApp().setCurrentPet(pet); if (getCurrentPages().length > 1) wx.navigateBack(); else wx.switchTab({ url: config.routes.home }); }
};

const petDetail = {
  title: '宠物档案', description: '它的资料与照护入口。', requiresPet: true, petIdFromId: true,
  load: (ctx) => {
    const id = ctx.options.id || ctx.pet.id;
    return Promise.all([pets.get(id), pets.access(id)]).then(([pet, access]) => {
      const permissions = access.permissions || [];
      const actions = [];
      if (access.role === 'owner') actions.push({ id: 'edit', label: '编辑资料', route: `${config.routes.petCreate}?id=${id}` });
      if (permissions.includes('CREATE_RECORD')) actions.push({ id: 'daily', label: '今日打卡', route: `${config.routes.dailyCheckin}?petId=${id}` });
      return {
        summary: `${pet.name} · ${pet.breed || '品种未填写'}\n${pet.type === 'cat' ? '猫咪' : pet.type === 'dog' ? '狗狗' : '伙伴'} · ${pet.gender || '性别未填写'}${pet.weight ? ` · ${pet.weight} kg` : ''}`,
        rows: [{ id: 'photos', title: '成长照片', subtitle: pet.vectorizationStatus === 'completed' ? '三视图已完成' : '照片识别处理中', meta: '查看档案照片', chevron: true }, { id: 'health', title: '健康记录', subtitle: '打卡、体重和就诊记录', meta: '', chevron: true }],
        actions
      };
    });
  },
  onRowTap: (ctx, row) => row.id === 'photos' ? wx.switchTab({ url: config.routes.album }) : wx.navigateTo({ url: config.routes.timeline })
};

const petTags = {
  title: '宠物标签', description: '整理常用标签，帮助记录更清楚。',
  fields: [{ key: 'name', label: '新标签', placeholder: '输入标签名称', required: true }, { key: 'category', label: '分类', type: 'select', options: [h.option('宠物', 'pet'), h.option('地点', 'location'), h.option('动作', 'action'), h.option('物品', 'object'), h.option('心情', 'mood'), h.option('其他', 'other')] }],
  load: () => tags.list().then((items) => ({ rows: h.simpleRows(items, (item) => item.name, (item) => item.category || '自定义标签', (item) => item.usageCount ? `使用 ${item.usageCount} 次` : '') })),
  submit: (ctx) => tags.create(ctx.form),
  successMessage: '标签已添加'
};

module.exports = { 'pet-list': petList, 'pet-create': petCreate, 'pet-detail': petDetail, 'pet-tags': petTags };
