function today() {
  const value = new Date();
  const pad = (number) => String(number).padStart(2, '0');
  return `${value.getFullYear()}-${pad(value.getMonth() + 1)}-${pad(value.getDate())}`;
}

function monthStart() { return `${today().slice(0, 7)}-01`; }
function addDays(date, amount) {
  const value = new Date(`${date}T12:00:00`);
  value.setDate(value.getDate() + amount);
  const pad = (number) => String(number).padStart(2, '0');
  return `${value.getFullYear()}-${pad(value.getMonth() + 1)}-${pad(value.getDate())}`;
}
function array(value) { return Array.isArray(value) ? value : []; }
function option(label, value) { return { label, value }; }
function simpleRows(items, title, subtitle, meta) {
  return array(items).map((item, index) => ({
    id: item.id || item.messageId || index,
    title: title(item) || '未命名记录',
    subtitle: subtitle ? subtitle(item) : '',
    meta: meta ? meta(item) : '',
    imageUrl: item.avatarUrl || item.fileUrl || '',
    chevron: true,
    raw: item
  }));
}

module.exports = { today, monthStart, addDays, array, option, simpleRows };
