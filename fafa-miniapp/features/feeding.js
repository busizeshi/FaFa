const config = require('../config');
const feeding = require('../services/feeding');
const h = require('./helpers');

const feedingFields = [
  { key: 'feedingDate', label: '喂食日期', type: 'date', defaultValue: h.today(), required: true },
  { key: 'feedingClock', label: '喂食时间', type: 'time', defaultValue: '08:00', required: true },
  { key: 'foodType', label: '食品类型', type: 'select', required: true, options: [h.option('主粮', 'main_food'), h.option('零食', 'snack'), h.option('罐头', 'can'), h.option('其他', 'other')] },
  { key: 'amount', label: '喂食量', type: 'number', placeholder: '选填' },
  { key: 'unit', label: '单位', type: 'select', options: [h.option('克', 'g'), h.option('毫升', 'ml')] },
  { key: 'foodNameSnapshot', label: '食品名称', placeholder: '选填' },
  { key: 'notes', label: '备注', type: 'textarea', maxlength: 500 }
];

const feedingToday = {
  title: '今日喂养', description: '按日常节奏记录每一餐。', requiresPet: true,
  load: (ctx) => Promise.all([feeding.records.list(ctx.pet.id, h.today()), feeding.plans.list(ctx.pet.id, 'active')]).then(([records, plans]) => ({
    summary: ctx.options.mode === 'record' ? '记录完成的一餐，实际喂食情况会用于后续分析。' : `今天已记录 ${h.array(records).length} 餐 · ${h.array(plans).length} 个进行中的计划`,
    rows: ctx.options.mode === 'record' ? [] : h.simpleRows(records, (item) => item.foodNameSnapshot || ({ main_food: '主粮', snack: '零食', can: '罐头', other: '其他' })[item.foodType] || '喂食记录', (item) => `${item.amount || '未记录数量'} ${item.unit || ''}`, (item) => (item.feedingTime || '').slice(11, 16) || '今日'),
    fields: ctx.options.mode === 'record' ? feedingFields : [],
    actions: ctx.options.mode === 'record' ? [] : [
      { id: 'add-feeding', label: '记录一餐', route: `${config.routes.feedingToday}?mode=record` },
      { id: 'plans', label: '喂养计划', route: config.routes.planList }
    ]
  })),
  submit: (ctx) => feeding.records.create({
    petId: ctx.pet.id,
    feedingTime: `${ctx.form.feedingDate || h.today()}T${ctx.form.feedingClock || '08:00'}:00`,
    foodType: ctx.form.foodType,
    amount: ctx.form.amount ? Number(ctx.form.amount) : null,
    unit: ctx.form.unit || 'g',
    foodNameSnapshot: ctx.form.foodNameSnapshot || '',
    notes: ctx.form.notes || ''
  }),
  afterSubmit: (_, ctx) => wx.navigateBack(),
  actions: [
    { id: 'add-feeding', label: '记录一餐', route: `${config.routes.feedingToday}?mode=record` },
    { id: 'plans', label: '喂养计划', route: config.routes.planList }
  ]
};

const planList = {
  title: '喂养计划', description: '适合它的节奏，可以从小处慢慢调整。', requiresPet: true,
  load: (ctx) => feeding.plans.list(ctx.pet.id).then((items) => ({
    rows: h.simpleRows(items, (item) => item.name || '日常喂养计划', (item) => `${item.status || '草稿'} · ${item.dailyTargetAmount || '—'} ${item.unit || 'g'}/日`, (item) => `${item.startDate || ''}${item.endDate ? ` 至 ${item.endDate}` : ' · 长期'}`).map((row, index) => Object.assign(row, { raw: items[index], chevron: false, actionLabel: items[index].status === 'active' ? '暂停' : items[index].status === 'paused' ? '恢复' : '' }))
  })),
  actions: [{ id: 'create-plan', label: '新建计划', primary: true, route: config.routes.planCreate }],
  onRowAction: (ctx, row) => row.raw.status === 'active' ? feeding.plans.pause(row.id) : feeding.plans.resume(row.id)
};

const planCreate = {
  title: '喂养计划', description: '选择食品并设定每日目标。', requiresPet: true,
  fields: [
    { key: 'name', label: '计划名称', placeholder: '例如：日常主粮计划' },
    { key: 'startDate', label: '开始日期', type: 'date', defaultValue: h.today(), required: true },
    { key: 'endDate', label: '结束日期', type: 'date', placeholder: '不填表示长期' },
    { key: 'foodId', label: '食品档案', type: 'select', required: true, options: [] },
    { key: 'dailyTargetAmount', label: '每日目标喂食量', type: 'number', required: true, placeholder: '克或毫升' },
    { key: 'dailyTargetCalories', label: '每日目标能量（kcal）', type: 'number', placeholder: '选填' },
    { key: 'mealTime', label: '喂食时间', type: 'time', defaultValue: '08:00', required: true },
    { key: 'unit', label: '单位', type: 'select', options: [h.option('克', 'g'), h.option('毫升', 'ml')] },
    { key: 'notes', label: '备注', type: 'textarea', maxlength: 500 }
  ],
  load: (ctx) => feeding.foods.list(ctx.pet.id, 'active').then((items) => ({
    fields: planCreate.fields.map((field) => field.key === 'foodId' ? Object.assign({}, field, { options: h.array(items).map((food) => h.option(food.name, food.id)) }) : field),
    summary: items.length ? '每日计划至少包含一项食品。' : '请先在食品库建立食品档案，再创建喂养计划。'
  })),
  submit: (ctx) => feeding.plans.create({
    petId: ctx.pet.id,
    name: ctx.form.name,
    startDate: ctx.form.startDate,
    endDate: ctx.form.endDate || null,
    dailyTargetAmount: Number(ctx.form.dailyTargetAmount),
    dailyTargetCalories: ctx.form.dailyTargetCalories ? Number(ctx.form.dailyTargetCalories) : null,
    unit: ctx.form.unit || 'g',
    recommendationSource: 'user_input',
    items: [{ itemKey: 'meal-1', foodId: Number(ctx.form.foodId), mealTime: ctx.form.mealTime || '08:00', amount: Number(ctx.form.dailyTargetAmount), unit: ctx.form.unit || 'g', percentage: 100, cycleType: 'daily' }],
    notes: ctx.form.notes || ''
  }),
  afterSubmit: () => wx.navigateBack()
};

const analysis = {
  title: '喂养分析', description: '用连续记录辅助日常观察，不替代专业建议。', requiresPet: true,
  load: (ctx) => feeding.analysis(ctx.pet.id, h.addDays(h.today(), -29), h.today()).then((result) => {
    const facts = result.facts || {};
    const weight = facts.weight || {};
    const daily = facts.dailyStatus || {};
    const observations = h.array(result.observations).map((item) => ({
      id: item.key,
      title: item.text || item.key || '喂养观察',
      subtitle: h.array(item.evidence).join(' · '),
      meta: '分析观察',
      chevron: false
    }));
    const suggestions = h.array(result.reviewSuggestions).map((text, index) => ({ id: `suggestion-${index}`, title: text, subtitle: '', meta: '复查建议', chevron: false }));
    const rate = facts.planCompletionRate === null || facts.planCompletionRate === undefined ? '' : ` · 计划完成率 ${Math.round(Number(facts.planCompletionRate) * 100)}%`;
    const weightSummary = weight.sampleCount ? ` · 体重变化 ${weight.changeKg > 0 ? '+' : ''}${weight.changeKg} kg` : '';
    return {
      summary: `近 30 天记录 ${facts.feedingDays || 0} 天 · 日均 ${facts.averageDailyAmount || 0} ${facts.unit || 'g'}${rate}${weightSummary}\n食欲正常 ${daily.appetiteNormalDays || 0} 天 · 软便 ${daily.softStoolDays || 0} 天 · 拒食 ${daily.refusalDays || 0} 天${h.array(result.limitations).length ? `\n数据说明：${h.array(result.limitations).join('；')}` : ''}`,
      rows: observations.concat(suggestions)
    };
  })
};

const foodLibrary = {
  title: '食品档案', description: '记录食品信息，方便查看喂养与换粮情况。', requiresPet: true,
  load: (ctx) => feeding.foods.list(ctx.pet.id).then((items) => ({ rows: h.simpleRows(items, (item) => item.name, (item) => `${item.brand || '品牌未填写'} · ${item.foodType || ''}`, (item) => item.status || item.specification || '') })),
  actions: [
    { id: 'recognize-food', label: '拍照识别包装', primary: true },
    { id: 'transition', label: '换粮计划', route: config.routes.foodTransition }
  ],
  onRowTap: (ctx, row) => wx.navigateTo({ url: `${config.routes.foodDetail}?id=${row.id}` })
};

const foodDetail = {
  title: '食品详情', description: '安全评分是辅助信息，不代表产品认证。', requiresPet: true,
  load: (ctx) => Promise.all([feeding.foods.get(ctx.options.id), feeding.foods.safety(ctx.options.id, ctx.pet.id)]).then(([food, score]) => ({
    summary: `${food.name}\n${food.brand || '品牌未填写'} · ${food.specification || '规格未填写'}\n${score.summary || score.explanation || '食品信息请以包装标注为准。'}`,
    rows: h.simpleRows(food.ingredients || [], (item) => typeof item === 'string' ? item : item.name, () => '', () => '配料表')
  }))
};

const foodRecognition = {
  title: '食品包装识别', description: '先识别包装信息，再核对后保存。', requiresPet: true,
  actions: [{ id: 'recognize-food', label: '拍摄或选择包装照片', primary: true }],
  fields: [
    { key: 'name', label: '食品名称', required: true, placeholder: '请核对包装名称' },
    { key: 'brand', label: '品牌', placeholder: '选填' },
    { key: 'foodType', label: '食品类型', type: 'select', required: true, options: [h.option('主食', 'main_food'), h.option('罐头', 'canned'), h.option('零食', 'snack'), h.option('补充剂', 'supplement'), h.option('自制', 'homemade'), h.option('其他', 'other')] },
    { key: 'specification', label: '包装规格', placeholder: '选填' },
    { key: 'notes', label: '备注', type: 'textarea', maxlength: 500 }
  ],
  submit: (ctx) => ctx.form.recognitionId
    ? feeding.recognition.confirm(ctx.form.recognitionId, { name: ctx.form.name, brand: ctx.form.brand, foodType: ctx.form.foodType, specification: ctx.form.specification, notes: ctx.form.notes })
    : Promise.reject({ message: '请先拍摄或选择食品包装照片' }),
  afterSubmit: () => wx.navigateTo({ url: config.routes.foodLibrary })
};

const transition = {
  title: '换粮计划', description: '循序渐进调整比例，并留意身体反应。', requiresPet: true,
  createFields: [
    { key: 'oldFoodId', label: '当前食品', type: 'select', required: true, options: [] },
    { key: 'newFoodId', label: '新食品', type: 'select', required: true, options: [] },
    { key: 'startDate', label: '开始日期', type: 'date', defaultValue: h.today(), required: true },
    { key: 'endDate', label: '结束日期', type: 'date', required: true },
    { key: 'dailyTargetAmount', label: '每日总量', type: 'number', required: true },
    { key: 'ratioStepsText', label: '比例阶段', type: 'textarea', required: true, placeholder: '每行填写：YYYY-MM-DD,旧粮比例（%）\n例如：2026-10-07,75\n新粮比例会自动补足到 100%' }
  ],
  load: (ctx) => Promise.all([feeding.foods.list(ctx.pet.id, 'active'), feeding.transitions.list(ctx.pet.id)]).then(([foods, transitions]) => {
    const options = h.array(foods).map((food) => h.option(food.name, food.id));
    if (ctx.options.mode === 'create') return { fields: transition.createFields.map((field) => ['oldFoodId', 'newFoodId'].includes(field.key) ? Object.assign({}, field, { options }) : field), summary: '每日旧粮与新粮比例按第一阶段设置，开始日期后生效。' };
    const rows = h.simpleRows(transitions, (item) => `${item.oldFoodName || '旧食品'} → ${item.newFoodName || '新食品'}`, (item) => `${item.status || ''} · ${item.startDate || ''} 至 ${item.endDate || ''}`, (item) => '查看每日比例并留意身体反应').map((row, index) => Object.assign(row, { raw: transitions[index], actionLabel: transitions[index].status === 'active' ? '完成' : '' }));
    return { rows, actions: [{ id: 'create-transition', label: '新建换粮计划', primary: true, route: `${config.routes.foodTransition}?mode=create` }] };
  }),
  submit: (ctx) => {
    const ratioSteps = (ctx.form.ratioStepsText || '').split(/\r?\n/).map((line) => line.trim()).filter(Boolean).map((line) => {
      const parts = line.split(/[,，\s]+/).filter(Boolean);
      return { date: parts[0], oldPercent: Number(parts[1]), newPercent: 100 - Number(parts[1]) };
    });
    const validDate = (value) => /^\d{4}-\d{2}-\d{2}$/.test(value);
    if (!ratioSteps.length || ratioSteps.some((step) => !validDate(step.date) || !Number.isFinite(step.oldPercent) || step.oldPercent < 0 || step.oldPercent > 100)) {
      return Promise.reject({ message: '请按 YYYY-MM-DD,旧粮比例 的格式填写阶段，比例需在 0 到 100 之间' });
    }
    if (ctx.form.endDate < ctx.form.startDate || Number(ctx.form.dailyTargetAmount) <= 0 || Number(ctx.form.oldFoodId) === Number(ctx.form.newFoodId)) {
      return Promise.reject({ message: '请检查换粮日期、每日总量，并确保新旧食品不同' });
    }
    if (ratioSteps.some((step) => step.date < ctx.form.startDate || step.date > ctx.form.endDate)) {
      return Promise.reject({ message: '每个比例阶段日期都需位于计划起止日期内' });
    }
    return feeding.transitions.create({
      petId: ctx.pet.id, oldFoodId: Number(ctx.form.oldFoodId), newFoodId: Number(ctx.form.newFoodId),
      startDate: ctx.form.startDate, endDate: ctx.form.endDate, dailyTargetAmount: Number(ctx.form.dailyTargetAmount), unit: 'g',
      ratioSteps: ratioSteps.sort((left, right) => left.date.localeCompare(right.date))
    });
  },
  onRowAction: (ctx, row) => feeding.transitions.complete(row.id),
  successMessage: '换粮计划已保存'
};

module.exports = {
  'feeding-today': feedingToday, 'plan-list': planList, 'plan-create': planCreate,
  'feeding-analysis': analysis, 'food-library': foodLibrary, 'food-detail': foodDetail,
  'food-recognition': foodRecognition, 'food-transition': transition
};
