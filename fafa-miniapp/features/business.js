const config = require('../config');
const users = require('../services/user');
const pets = require('../services/pet');
const business = require('../services/business');
const h = require('./helpers');

const reports = {
  title: '健康报告', description: '把连续记录整理成阶段回顾。AI 内容仅供日常参考。', requiresPet: true,
  load: (ctx) => business.reports.list(ctx.pet.id).then((items) => ({ rows: h.simpleRows(items, (item) => item.title || `${item.periodType || '周期'}报告`, (item) => item.summary || item.status || '', (item) => item.periodStart ? `${item.periodStart} 至 ${item.periodEnd || ''}` : item.createdAt || '') })),
  actions: [
    { id: 'generate', label: '生成本月报告', primary: true, run: (ctx) => business.reports.generate({ petId: ctx.pet.id, periodType: 'month', anchorDate: h.today(), force: false }) },
    { id: 'content', label: '分享文案', route: config.routes.contentDrafts }
  ],
  onRowTap: (ctx, row) => wx.navigateTo({ url: `${config.routes.reportDetail}?id=${row.id}` })
};

const reportDetail = {
  title: '报告详情', description: 'AI 汇总用于辅助回顾，不是诊断或治疗建议。', requiresPet: false, petIdFromId: false,
  load: (ctx) => business.reports.get(ctx.options.id).then((report) => ({
    summary: typeof report.content === 'string' ? report.content : JSON.stringify(report.content || report, null, 2),
    actions: [{ id: 'regenerate', label: '重新生成', run: () => business.reports.regenerate(ctx.options.id) }]
  }))
};

const family = {
  title: '家庭成员', description: '一起照护宠物，权限由服务端统一管理。', requiresPet: true,
  fields: [{ key: 'role', label: '邀请身份', type: 'select', required: true, options: [h.option('家庭成员', 'member'), h.option('照护者', 'caregiver')] }, { key: 'expiresInDays', label: '有效天数', type: 'number', defaultValue: '7', required: true }],
  load: (ctx) => ctx.options.view === 'audit'
    ? pets.auditLogs(ctx.pet.id).then((items) => ({ title: '访问记录', fields: [], rows: h.simpleRows(items.records || items, (item) => item.actorName || item.action || '家庭访问', (item) => item.resource || item.details || '', (item) => item.createdAt || '') }))
    : pets.members(ctx.pet.id).then((items) => ({ rows: h.simpleRows(items, (item) => item.displayName || item.nickname || item.userName || '家庭成员', (item) => item.role || '', (item) => item.status || '已加入') })),
  actions: [{ id: 'audit', label: '查看访问记录', route: `${config.routes.family}?view=audit` }],
  submit: (ctx) => pets.invite(ctx.pet.id, { role: ctx.form.role, expiresInDays: ctx.form.role === 'caregiver' ? Number(ctx.form.expiresInDays) : null }),
  afterSubmit: (invite, ctx) => ctx.page.setData({ summary: `邀请编号：${invite.invitationId || '—'}\n一次性凭证：${invite.token || invite.rawToken || invite.invitationToken || '请从邀请返回结果中复制凭证'}\n有效期至：${invite.expiresAt || '—'}` })
};

const expenseBook = {
  title: '照护账本', description: '查看宠物相关开支与分类汇总。', requiresPet: true,
  load: (ctx) => Promise.all([
    business.expenses.statistics({ petId: ctx.pet.id, period: 'month', anchorDate: h.today() }),
    business.expenses.list({ petId: ctx.pet.id, startDate: h.monthStart(), endDate: h.today(), page: 1, size: config.request.pageSize })
  ]).then(([stats, result]) => ({
    summary: `本月支出 ${stats.totalAmount || 0} · 共 ${stats.expenseCount || 0} 笔`,
    rows: h.simpleRows(result.records, (item) => item.title, (item) => `${item.categoryName || item.category || ''} · ${item.amount} ${item.currency || 'CNY'}`, (item) => item.expenseDate || '')
  })),
  actions: [{ id: 'add-expense', label: '记一笔', primary: true, route: config.routes.expenseCreate }]
};

const expenseCreate = {
  title: '新增开支', description: '记录发生日期、分类和金额。', requiresPet: true,
  fields: [
    { key: 'title', label: '开支名称', required: true, placeholder: '例如：购买主粮' },
    { key: 'category', label: '开支分类', type: 'select', required: true, options: [] },
    { key: 'amount', label: '金额', type: 'number', required: true, placeholder: '0.00' },
    { key: 'expenseDate', label: '发生日期', type: 'date', defaultValue: h.today(), required: true },
    { key: 'notes', label: '备注', type: 'textarea', maxlength: 500 }
  ],
  load: () => business.expenses.categories().then((items) => ({ fields: expenseCreate.fields.map((field) => field.key === 'category' ? Object.assign({}, field, { options: h.array(items).map((item) => h.option(item.name, item.code || item.category)) }) : field) })),
  submit: (ctx) => business.expenses.create(Object.assign({}, ctx.form, { petId: ctx.pet.id, amount: Number(ctx.form.amount), currency: 'CNY' })),
  afterSubmit: () => wx.navigateBack()
};

const membership = {
  title: 'FaFa 会员', description: '查看当前会员状态与服务权益。',
  load: () => Promise.all([users.getVip(), users.getQuota()]).then(([vip, quota]) => ({
    summary: `${vip.vip || vip.isVip ? '当前为会员' : '当前为免费用户'}${vip.vipExpireTime ? ` · 有效期至 ${vip.vipExpireTime.slice(0, 10)}` : ''}\n${quota ? '服务额度以当前账户和服务端返回为准。' : ''}`,
    rows: [{ id: 'payment', title: '会员购买', subtitle: '线上支付尚未接通', meta: '当前不能在小程序内完成升级或续费', chevron: false }]
  }))
};

const profile = {
  title: '个人资料', description: '更新后会同步到你的账户资料。',
  actions: [{ id: 'change-avatar', label: '更换头像' }],
  fields: [
    { key: 'nickname', label: '昵称', maxlength: 50 },
    { key: 'phone', label: '手机号', type: 'number', placeholder: '选填' },
    { key: 'gender', label: '性别', type: 'select', options: [h.option('男', '男'), h.option('女', '女'), h.option('未知', '未知')] },
    { key: 'age', label: '年龄', type: 'number' },
    { key: 'city', label: '城市', maxlength: 50 }
  ],
  load: () => users.getMe().then((user) => ({ form: { nickname: user.nickname || '', phone: user.phone || '', gender: user.gender || '未知', age: user.age === null ? '' : String(user.age || ''), city: user.city || '' } })),
  submit: (ctx) => users.updateProfile({ nickname: ctx.form.nickname, phone: ctx.form.phone || null, gender: ctx.form.gender || '未知', age: ctx.form.age ? Number(ctx.form.age) : null, city: ctx.form.city || '' }),
  afterSubmit: (user) => { wx.setStorageSync(config.storage.user, user); getApp().globalData.user = user; wx.navigateBack(); }
};

const emergency = {
  title: '紧急资料', description: '仅填写需要在紧急情况下使用的信息。', requiresPet: true,
  fields: [
    { key: 'version', label: '资料版本', type: 'number', defaultValue: '0', required: true },
    { key: 'bloodType', label: '血型' }, { key: 'allergies', label: '过敏史', type: 'textarea' },
    { key: 'chronicConditions', label: '慢性病或病史', type: 'textarea' },
    { key: 'emergencyContactName', label: '紧急联系人' }, { key: 'emergencyContactPhone', label: '联系人电话', type: 'number' },
    { key: 'primaryHospitalName', label: '首选医院' }, { key: 'primaryHospitalPhone', label: '医院电话', type: 'number' },
    { key: 'primaryHospitalAddress', label: '医院地址', type: 'textarea' }, { key: 'notes', label: '补充备注', type: 'textarea' }
  ],
  load: (ctx) => ctx.options.view === 'card'
    ? business.emergency.card(ctx.pet.id).then((card) => ({ title: '宠物急救卡', fields: [], summary: JSON.stringify(card, null, 2) }))
    : business.emergency.getProfile(ctx.pet.id).then((record) => ({ form: Object.assign({}, record, { version: String(record.version || 0) }), summary: '资料仅向有权限的家庭成员展示。' })),
  submit: (ctx) => business.emergency.updateProfile(ctx.pet.id, Object.assign({}, ctx.form, { version: Number(ctx.form.version) })),
  actions: [{ id: 'view-card', label: '预览急救卡', route: `${config.routes.emergency}?view=card` }],
  afterSubmit: () => wx.navigateBack()
};

const invitationAccept = {
  title: '接受邀请', description: '请输入邀请消息中的编号和一次性凭证。',
  fields: [{ key: 'invitationId', label: '邀请编号', type: 'number', required: true }, { key: 'token', label: '邀请凭证', required: true }],
  submit: (ctx) => pets.acceptInvitation({ invitationId: Number(ctx.form.invitationId), token: ctx.form.token }),
  afterSubmit: () => wx.switchTab({ url: config.routes.home })
};

const contentDrafts = {
  title: '分享文案', description: 'AI 生成内容需自行核对并确认后再分享。', requiresPet: true,
  load: (ctx) => business.content.list(ctx.pet.id).then((items) => ({ rows: h.simpleRows(items, (item) => item.title || item.contentType || '内容草稿', (item) => item.status || '', (item) => item.createdAt || '') })),
  actions: [
    { id: 'create-draft', label: '生成月度回顾', primary: true, run: (ctx) => business.content.create({ petId: ctx.pet.id, contentType: 'share_note', topicType: 'monthly_summary', periodType: 'month', anchorDate: h.today(), idempotencyKey: `draft-${Date.now()}` }) },
    { id: 'schedule', label: '周期设置', route: config.routes.contentSchedule }
  ],
  onRowTap: (ctx, row) => wx.navigateTo({ url: `${config.routes.contentEdit}?id=${row.id}` })
};

const contentEdit = {
  title: '编辑分享文案', description: '发布前请核对事实、隐私和健康相关表述。',
  fields: [{ key: 'content', label: '文案内容', type: 'textarea', maxlength: 5000, required: true }],
  load: (ctx) => business.content.get(ctx.options.id).then((draft) => ({ form: { content: typeof draft.draft === 'string' ? draft.draft : JSON.stringify(draft.draft || draft.content || '', null, 2) } })),
  submit: (ctx) => business.content.update(ctx.options.id, { draft: { text: ctx.form.content } }),
  actions: [{ id: 'regenerate', label: '重新生成', run: (ctx) => business.content.regenerate(ctx.options.id) }]
};

const contentSchedule = {
  title: '周期内容设置', description: '管理自动生成内容的时间间隔。', requiresPet: true,
  fields: [
    { key: 'contentType', label: '内容类型', type: 'select', options: [h.option('分享笔记', 'share_note')] },
    { key: 'topicType', label: '主题', type: 'select', options: [h.option('月度回顾', 'monthly_summary')] },
    { key: 'intervalDays', label: '间隔天数', type: 'number', defaultValue: '15', required: true }
  ],
  load: (ctx) => business.content.schedules(ctx.pet.id).then((items) => ({ rows: h.simpleRows(items, (item) => item.topicType || '周期内容', (item) => item.enabled ? '已启用' : '已暂停', (item) => item.nextRunAt || '').map((row, index) => Object.assign(row, { raw: items[index], chevron: false, actionLabel: items[index].enabled ? '暂停' : '启用' })) })),
  submit: (ctx) => business.content.schedule(Object.assign({}, ctx.form, { petId: ctx.pet.id, intervalDays: Number(ctx.form.intervalDays) })),
  onRowAction: (ctx, row) => row.raw.enabled ? business.content.disableSchedule(row.id) : business.content.enableSchedule(row.id)
};

module.exports = {
  'ai-reports': reports, 'ai-report-detail': reportDetail, 'family-members': family,
  'expense-book': expenseBook, 'expense-create': expenseCreate, membership, 'profile-edit': profile,
  emergency, 'invitation-accept': invitationAccept, 'content-drafts': contentDrafts,
  'content-edit': contentEdit, 'content-schedule': contentSchedule
};
