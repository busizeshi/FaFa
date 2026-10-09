const config = require('../config');
const care = require('../services/care');
const medical = require('../services/medical');
const h = require('./helpers');

const visits = {
  title: '就诊记录', description: '把每次检查、处方和复诊安排留在档案里。', requiresPet: true,
  load: (ctx) => medical.visits.list({ petId: ctx.pet.id, startDate: h.addDays(h.today(), -364), endDate: h.today(), page: 1, size: config.request.pageSize }).then((result) => ({
    rows: h.simpleRows(result.records, (item) => item.hospitalName || '就诊记录', (item) => item.diagnosis || item.chiefComplaint || '查看诊疗详情', (item) => item.visitDate || ''),
    summary: `共 ${result.total || 0} 条就诊记录`
  })),
  actions: [{ id: 'add-visit', label: '新增就诊记录', primary: true, route: config.routes.medicalCreate }],
  onRowTap: (ctx, row) => wx.navigateTo({ url: `${config.routes.medicalCreate}?id=${row.id}` })
};

const visitFields = [
  { key: 'visitDate', label: '就诊日期', type: 'date', defaultValue: h.today(), required: true },
  { key: 'hospitalName', label: '医院名称', required: true },
  { key: 'doctorName', label: '接诊医生' },
  { key: 'chiefComplaint', label: '本次问题', type: 'textarea', maxlength: 1000 },
  { key: 'diagnosis', label: '诊断结果', type: 'textarea', maxlength: 2000 },
  { key: 'prescription', label: '处方内容', type: 'textarea', maxlength: 3000 },
  { key: 'treatment', label: '治疗/处置', type: 'textarea', maxlength: 2000 },
  { key: 'totalCost', label: '本次费用', type: 'number' },
  { key: 'notes', label: '备注', type: 'textarea', maxlength: 2000 }
];

const visitCreate = {
  title: '新增就诊记录', description: '保存资料方便后续复诊时回看。', requiresPet: true,
  fields: visitFields,
  load: (ctx) => ctx.options.id ? medical.visits.get(ctx.options.id).then((record) => ({ title: '就诊记录详情', form: record })) : Promise.resolve(null),
  submit: (ctx) => ctx.options.id
    ? medical.visits.update(ctx.options.id, ['hospitalName', 'doctorName', 'chiefComplaint', 'diagnosis', 'prescription', 'treatment', 'totalCost', 'notes'].reduce((data, key) => { data[key] = ctx.form[key]; return data; }, {}))
    : medical.visits.create(Object.assign({}, ctx.form, { petId: ctx.pet.id })),
  afterSubmit: () => wx.navigateBack()
};

const medicationPlans = {
  title: '用药计划', description: '记录计划与实际执行情况。', requiresPet: true,
  load: (ctx) => ctx.options.mode === 'create'
    ? Promise.resolve({ title: '新建用药计划', summary: '用药剂量和频率请按兽医处方填写。', fields: medicationPlanFields })
    : medical.medication.list(ctx.pet.id).then((items) => ({
      rows: h.simpleRows(items, (item) => item.medicationName, (item) => `${item.dosageValue || ''} ${item.dosageUnit || ''} · ${item.status || ''}`, (item) => `${item.startDate || ''}${item.endDate ? ` 至 ${item.endDate}` : ''}`).map((row, index) => Object.assign(row, { raw: items[index], chevron: false, actionLabel: ['active', 'paused'].includes(items[index].status) ? items[index].status === 'active' ? '暂停' : '恢复' : '' })),
      actions: [{ id: 'create-medication', label: '新建用药计划', primary: true, route: `${config.routes.medicationPlan}?mode=create` }, { id: 'visits', label: '就诊记录', route: config.routes.medicalRecords }]
    })),
  fields: [],
  submit: (ctx) => {
    const administrationTimes = (ctx.form.administrationTimes || '').split(',').map((time) => time.trim()).filter(Boolean);
    if (administrationTimes.length > 6 || administrationTimes.some((time) => !/^([01]\d|2[0-3]):[0-5]\d$/.test(time))) {
      return Promise.reject({ message: '服药时间请使用 HH:mm 格式，最多填写 6 个时间' });
    }
    if (Number(ctx.form.dosageValue) <= 0 || Number(ctx.form.frequencyValue) <= 0) {
      return Promise.reject({ message: '单次剂量与频率数值必须大于 0' });
    }
    if (ctx.form.startDate > h.today() || (ctx.form.endDate && ctx.form.endDate < ctx.form.startDate)) {
      return Promise.reject({ message: '用药开始日期不能晚于今天，结束日期也不能早于开始日期' });
    }
    return medical.medication.create({
      petId: ctx.pet.id,
      sourceVisitId: ctx.form.sourceVisitId ? Number(ctx.form.sourceVisitId) : null,
      medicationName: ctx.form.medicationName,
      specification: ctx.form.specification || '',
      purpose: ctx.form.purpose || '',
      dosageValue: Number(ctx.form.dosageValue),
      dosageUnit: ctx.form.dosageUnit,
      frequencyType: ctx.form.frequencyType,
      frequencyValue: Number(ctx.form.frequencyValue),
      administrationTimes,
      mealRelation: ctx.form.mealRelation,
      startDate: ctx.form.startDate,
      endDate: ctx.form.endDate || null,
      notes: ctx.form.notes || ''
    });
  },
  afterSubmit: () => wx.navigateBack(),
  onRowAction: (ctx, row) => row.raw.status === 'active' ? medical.medication.pause(row.id) : medical.medication.resume(row.id)
};

const medicationPlanFields = [
  { key: 'sourceVisitId', label: '关联就诊记录编号', type: 'number', placeholder: '选填' },
  { key: 'medicationName', label: '药品名称', required: true },
  { key: 'specification', label: '药品规格', placeholder: '选填' },
  { key: 'purpose', label: '用药目的', type: 'textarea', maxlength: 500 },
  { key: 'dosageValue', label: '单次用量', type: 'number', required: true },
  { key: 'dosageUnit', label: '剂量单位', required: true, placeholder: '例如：粒、ml' },
  { key: 'frequencyType', label: '用药频率', type: 'select', defaultValue: 'daily', required: true, options: [h.option('每天', 'daily'), h.option('间隔天数', 'every_days'), h.option('每周', 'weekly')] },
  { key: 'frequencyValue', label: '频率数值', type: 'number', defaultValue: '1', required: true, placeholder: '例如：每 2 天填写 2' },
  { key: 'administrationTimes', label: '服药时间', defaultValue: '08:00', required: true, placeholder: '多个时间用逗号分隔，最多 6 个' },
  { key: 'mealRelation', label: '与进餐关系', type: 'select', required: true, options: [h.option('餐前', 'before_meal'), h.option('餐后', 'after_meal'), h.option('随餐', 'with_meal'), h.option('不限', 'anytime')] },
  { key: 'startDate', label: '开始日期', type: 'date', defaultValue: h.today(), required: true },
  { key: 'endDate', label: '结束日期', type: 'date', placeholder: '不填表示长期' },
  { key: 'notes', label: '备注', type: 'textarea', maxlength: 1000 }
];

const healthAlerts = {
  title: '健康提醒', description: '以下观察仅供参考，不能替代兽医诊断。', requiresPet: true,
  load: (ctx) => care.alerts.list(ctx.pet.id).then((items) => ({ rows: h.simpleRows(items, (item) => item.title, (item) => item.message || '', (item) => `${item.severity || ''} · ${item.observedFrom || ''}–${item.observedTo || ''}`).map((row, index) => Object.assign(row, { raw: items[index], chevron: false, actionLabel: items[index].status === 'open' ? '我已知晓' : items[index].status === 'acknowledged' ? '已处理' : '' })) })),
  onRowAction: (ctx, row) => row.raw.status === 'open' ? care.alerts.acknowledge(row.id) : care.alerts.resolve(row.id)
};

const reminders = {
  title: '提醒中心', description: '记下每个照护时刻，完成后会自动留痕。', requiresPet: false,
  load: () => care.reminders.today().then((items) => ({
    summary: `今天有 ${h.array(items).filter((item) => !item.completedToday).length} 项待完成`,
    rows: h.simpleRows(items, (item) => item.reminder && item.reminder.title || '照护提醒', (item) => item.reminder && item.reminder.petName || '', (item) => item.reminder && item.reminder.nextRemindTime || item.completionTime || '').map((row, index) => Object.assign(row, { id: items[index].reminder && items[index].reminder.id, raw: items[index], chevron: false, actionLabel: items[index].completedToday ? '已完成' : '完成' }))
  })),
  actions: [{ id: 'new-reminder', label: '新建提醒', primary: true, route: config.routes.reminderCreate }],
  onRowAction: (ctx, row) => row.raw.completedToday ? Promise.resolve() : care.reminders.complete(row.id, {})
};

const reminderFields = [
  { key: 'type', label: '提醒类型', type: 'select', required: true, options: ['疫苗|vaccine', '驱虫|deworming', '洗澡|bath', '修剪指甲|nail_trim', '体检|checkup', '用药|medication', '生日|birthday', '日常记录|daily_record', '自定义|custom'].map((item) => { const [label, value] = item.split('|'); return h.option(label, value); }) },
  { key: 'title', label: '提醒标题', required: true, placeholder: '例如：年度体检' },
  { key: 'description', label: '说明', type: 'textarea', maxlength: 500 },
  { key: 'intervalType', label: '重复周期', type: 'select', required: true, options: [h.option('仅一次', 'once'), h.option('按天', 'days'), h.option('每周', 'weekly'), h.option('每月', 'monthly'), h.option('间隔月', 'months'), h.option('每年', 'yearly')] },
  { key: 'intervalValue', label: '周期数值', type: 'number', placeholder: '例如：3 个月' },
  { key: 'remindDate', label: '下次提醒日期', type: 'date', defaultValue: h.today(), required: true },
  { key: 'remindTime', label: '提醒时间', type: 'time', defaultValue: '09:00', required: true }
];

const reminderCreate = {
  title: '设置提醒', description: '提醒会显示在小程序的提醒中心。', requiresPet: true,
  fields: reminderFields,
  submit: (ctx) => care.reminders.create({
    petId: ctx.pet.id, type: ctx.form.type, title: ctx.form.title, description: ctx.form.description,
    intervalType: ctx.form.intervalType, intervalValue: ctx.form.intervalValue ? Number(ctx.form.intervalValue) : null,
    nextRemindTime: `${ctx.form.remindDate}T${ctx.form.remindTime}:00`
  }),
  afterSubmit: () => wx.navigateBack()
};

const timeline = {
  title: '成长时间线', description: '把重要健康事件按时间串在一起。', requiresPet: true,
  load: (ctx) => medical.timeline({ petId: ctx.pet.id, startDate: h.addDays(h.today(), -364), endDate: h.today() }).then((items) => ({
    rows: h.simpleRows(items, (item) => item.title || item.type, (item) => item.summary || '', (item) => `${item.eventDate || ''} · ${item.status || ''}`)
  }))
};

const summary = {
  title: '就诊摘要', description: '将选定时间段的健康记录整理为摘要。', requiresPet: true,
  fields: [{ key: 'startDate', label: '开始日期', type: 'date', defaultValue: h.monthStart(), required: true }, { key: 'endDate', label: '结束日期', type: 'date', defaultValue: h.today(), required: true }, { key: 'includeMedia', label: '包含关联照片', type: 'select', options: [h.option('包含', true), h.option('不包含', false)] }],
  submit: (ctx) => medical.summary({ petId: ctx.pet.id, startDate: ctx.form.startDate, endDate: ctx.form.endDate, includeMedia: ctx.form.includeMedia === true }),
  afterSubmit: (result, ctx) => ctx.page.setData({ summary: `${result.plainText || result.overview || '摘要已生成，请结合原始就诊资料阅读。'}${h.array(result.attentionItems).length ? `\n重点关注：${h.array(result.attentionItems).join('；')}` : ''}${result.disclaimer ? `\n${result.disclaimer}` : ''}`, rows: [] })
};

module.exports = {
  'medical-records': visits, 'medical-create': visitCreate, 'medication-plan': medicationPlans,
  'health-alerts': healthAlerts, reminders, 'reminder-create': reminderCreate, 'growth-timeline': timeline, 'medical-summary': summary
};
