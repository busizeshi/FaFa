const config = require('../config');
const care = require('../services/care');
const h = require('./helpers');

const normalOptions = {
  appetite: [h.option('很好', 'excellent'), h.option('正常', 'normal'), h.option('下降', 'decreased'), h.option('拒食', 'refused')],
  energy: [h.option('活跃', 'active'), h.option('正常', 'normal'), h.option('没精神', 'lethargic')],
  waterIntake: [h.option('正常', 'normal'), h.option('较多', 'more'), h.option('较少', 'less'), h.option('未留意', 'not_noticed')],
  defecation: [h.option('正常', 'normal'), h.option('偏软', 'soft'), h.option('腹泻', 'diarrhea'), h.option('便秘', 'constipation'), h.option('未观察到', 'not_seen')],
  vomiting: [h.option('没有', 'none'), h.option('一次', 'once'), h.option('多次', 'multiple')],
  urination: [h.option('正常', 'normal'), h.option('异常', 'abnormal'), h.option('未观察到', 'not_seen')]
};

const dailyCheckin = {
  title: '今日照护打卡', description: '记录身体状态的变化，帮助更早发现异常。', requiresPet: true,
  fields: [
    ...Object.keys(normalOptions).map((key) => ({ key, label: ({ appetite: '食欲', energy: '精神活动', waterIntake: '饮水', defecation: '排便', vomiting: '呕吐', urination: '排尿' })[key], type: 'choices', options: normalOptions[key] })),
    { key: 'appetiteAmount', label: '进食量（克）', type: 'number', placeholder: '选填' },
    { key: 'waterAmount', label: '饮水量（毫升）', type: 'number', placeholder: '选填' },
    { key: 'notes', label: '备注', type: 'textarea', placeholder: '记录今天的小变化…', maxlength: 500 }
  ],
  load: (ctx) => Promise.all([care.daily.dimensions(ctx.pet.type), care.daily.get(ctx.pet.id, h.today())]).then(([dimensions, record]) => {
    const fields = h.array(dimensions).map((item) => ({
      key: item.dimension,
      label: item.label,
      hint: item.tip,
      type: 'choices',
      required: item.required,
      options: h.array(item.options).map((option) => h.option(option.label, option.value))
    }));
    fields.push({ key: 'appetiteAmount', label: '进食量（克）', type: 'number', placeholder: '选填' }, { key: 'waterAmount', label: '饮水量（毫升）', type: 'number', placeholder: '选填' }, { key: 'notes', label: '备注', type: 'textarea', placeholder: '记录今天的小变化…', maxlength: 500 });
    return {
      fields,
      summary: record ? (record.allNormal ? '今天的状态看起来都不错。' : `今日记录已保存${h.array(record.abnormalDimensions).length ? ` · 留意 ${h.array(record.abnormalDimensions).join('、')}` : ''}`) : '每个维度都可以跳过；有变化时再记录就好。',
      form: record || {}
    };
  }),
  actions: [
    { id: 'normal', label: '一切正常', run: (ctx) => care.daily.recordNormal(ctx.pet.id, h.today()) },
    { id: 'copy', label: '同上次', run: (ctx) => care.daily.copyLast(ctx.pet.id, h.today()) }
  ],
  submit: (ctx) => care.daily.save(Object.assign({}, ctx.form, { petId: ctx.pet.id, recordDate: h.today() }))
};

const dailyHistory = {
  title: '日常记录', description: '把每天的状态变化连起来看。', requiresPet: true,
  load: (ctx) => care.daily.list(ctx.pet.id, h.addDays(h.today(), -29), h.today()).then((rows) => ({
    summary: `${h.today().slice(0, 7)} · 共 ${h.array(rows).length} 天记录`,
    rows: h.simpleRows(rows, (item) => item.recordDate, (item) => item.allNormal ? '状态正常' : h.array(item.abnormalDimensions).join('、') || '记录已保存', (item) => item.notes || '日常照护')
  })),
  actions: [{ id: 'checkin', label: '今天去打卡', route: config.routes.dailyCheckin }]
};

const weightRecord = {
  title: '记录体重', description: '定期记录，观察长期变化会更有参考价值。', requiresPet: true,
  fields: [{ key: 'recordDate', label: '测量日期', type: 'date', defaultValue: h.today(), required: true }, { key: 'weight', label: '体重（kg）', type: 'number', placeholder: '例如：4.25', required: true }, { key: 'notes', label: '备注', type: 'textarea', placeholder: '选填', maxlength: 500 }],
  submit: (ctx) => care.weight.create(Object.assign({}, ctx.form, { petId: ctx.pet.id, weight: Number(ctx.form.weight) })),
  afterSubmit: () => wx.navigateBack()
};

const weightTrend = {
  title: '体重趋势', description: '结合连续记录观察成长变化。', requiresPet: true,
  load: (ctx) => care.weight.trend(ctx.pet.id, h.addDays(h.today(), -89), h.today()).then((trend) => ({
    summary: trend.latest ? `最近一次 ${trend.latest.recordDate} · ${trend.latest.weight} kg · 区间变化 ${Number(trend.totalChange || 0) > 0 ? '+' : ''}${trend.totalChange || 0} kg` : '记录几次体重后，这里会显示成长变化。',
    rows: h.simpleRows(trend.records, (item) => `${item.weight} kg`, (item) => item.recordDate, (item) => item.change === null || item.change === undefined ? item.notes || '首次记录' : `${Number(item.change) > 0 ? '+' : ''}${item.change} kg`)
  })),
  actions: [{ id: 'add-weight', label: '记录体重', primary: true, route: config.routes.weightRecord }]
};

module.exports = { 'daily-checkin': dailyCheckin, 'daily-history': dailyHistory, 'weight-record': weightRecord, 'weight-trend': weightTrend };
