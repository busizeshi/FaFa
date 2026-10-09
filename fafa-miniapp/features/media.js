const config = require('../config');
const media = require('../services/media');
const h = require('./helpers');

const upload = {
  title: '添加回忆', description: '照片与视频会归档到当前宠物的相册。', requiresPet: true,
  fields: [{ key: 'tags', label: '标签', placeholder: '用逗号分隔，例如：散步,公园' }],
  actions: [
    { id: 'select-media', label: '选择照片或视频' },
    { id: 'upload-media', label: '上传所选内容', primary: true }
  ],
  submit: (ctx, files) => media.photos.upload(files, { petId: ctx.pet.id, tags: ctx.form.tags || '' })
};

const detail = {
  title: '照片详情', description: '为这份回忆补充标签和时间。', requiresPet: true,
  load: (ctx) => media.photos.get(ctx.options.id).then((photo) => ({
    summary: `${photo.fileName || '照片'}\n${photo.aiDescription || photo.tags && photo.tags.join('、') || '还没有描述'}`,
    fields: [{ key: 'tagsText', label: '标签', type: 'textarea', placeholder: '用逗号分隔，例如：散步,公园', maxlength: 500 }],
    form: { tagsText: (photo.tags || []).join(',') },
    rows: [{ id: photo.id, title: photo.petName || '宠物归属未确认', subtitle: (photo.tags || []).join(' · ') || '暂无标签', meta: photo.takenAt || photo.createdAt || '', imageUrl: photo.fileUrl, chevron: false }],
    actions: [
      { id: 'search', label: '返回相册', tab: config.routes.album },
      { id: 'delete', label: '删除照片', run: () => new Promise((resolve, reject) => wx.showModal({ title: '删除这份回忆？', content: '删除后无法恢复。', success: (result) => result.confirm ? media.photos.remove(photo.id).then(() => resolve({ deleted: true })).catch(reject) : resolve({ deleted: false }) })), afterRun: (ctx, result) => { if (result && result.deleted) wx.navigateBack(); } }
    ]
  })),
  submit: (ctx) => media.photos.update(ctx.options.id, {
    tags: (ctx.form.tagsText || '').split(/[,，]/).map((tag) => tag.trim()).filter(Boolean)
  })
};

const search = {
  title: '搜索回忆', description: '留空可浏览相册；输入描述后会使用语义检索额度。', requiresPet: false,
  fields: [{ key: 'query', label: '描述这张照片', placeholder: '例如：它在阳台晒太阳' }],
  submit: (ctx) => media.photos.search({ query: ctx.form.query || '', petId: ctx.pet && ctx.pet.id, page: 1, size: config.request.pageSize }),
  afterSubmit: (result, ctx) => {
    const records = h.array(result && result.records);
    const total = result && result.total || 0;
    const page = result && result.page || 1;
    ctx.page.setData({ rows: h.simpleRows(records, (item) => item.petName || item.fileName || '照片', (item) => item.aiDescription || (item.tags || []).join(' · '), (item) => (item.takenAt || '').slice(0, 10)).map((row, index) => Object.assign(row, { imageUrl: records[index].fileUrl })), page, total, hasMore: page * config.request.pageSize < total });
  },
  onRowTap: (ctx, row) => wx.navigateTo({ url: `${config.routes.photoDetail}?id=${row.id}` })
};

module.exports = { 'media-upload': upload, 'photo-detail': detail, 'photo-search': search };
