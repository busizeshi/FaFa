const formatTime = date => {
  const year = date.getFullYear();
  const month = date.getMonth() + 1;
  const day = date.getDate();
  const hour = date.getHours();
  const minute = date.getMinutes();
  const second = date.getSeconds();

  return `${[year, month, day].map(formatNumber).join('/')} ${[hour, minute, second].map(formatNumber).join(':')}`;
};

const formatDate = date => {
  if (typeof date === 'string') {
    date = new Date(date);
  }
  const year = date.getFullYear();
  const month = date.getMonth() + 1;
  const day = date.getDate();

  return `${year}-${formatNumber(month)}-${formatNumber(day)}`;
};

const formatDateTime = date => {
  if (typeof date === 'string') {
    date = new Date(date);
  }
  const year = date.getFullYear();
  const month = date.getMonth() + 1;
  const day = date.getDate();
  const hour = date.getHours();
  const minute = date.getMinutes();

  return `${year}-${formatNumber(month)}-${formatNumber(day)} ${formatNumber(hour)}:${formatNumber(minute)}`;
};

const formatNumber = n => {
  n = n.toString();
  return n[1] ? n : `0${n}`;
};

const showSuccess = (title, duration = 1500) => {
  wx.showToast({
    title,
    icon: 'success',
    duration
  });
};

const showError = (title, duration = 1500) => {
  wx.showToast({
    title,
    icon: 'none',
    duration
  });
};

const showModal = (title, content) => {
  return new Promise((resolve) => {
    wx.showModal({
      title,
      content,
      success: (res) => {
        resolve(res.confirm);
      }
    });
  });
};

const getRecordTypeIcon = (type) => {
  const icons = {
    feeding: '🍖',
    water: '💧',
    weight: '⚖️',
    excretion: '💩',
    medication: '💊',
    vaccination: '💉',
    medical: '🏥',
    grooming: '✂️',
    training: '🎓',
    event: '📝'
  };
  return icons[type] || '📝';
};

const getRecordTypeText = (type) => {
  const texts = {
    feeding: '喂食',
    water: '饮水',
    weight: '体重',
    excretion: '排泄',
    medication: '用药',
    vaccination: '疫苗',
    medical: '就医',
    grooming: '美容',
    training: '训练',
    event: '事件'
  };
  return texts[type] || '记录';
};

const calculateAge = (birthday) => {
  if (!birthday) return '';
  
  const birth = new Date(birthday);
  const now = new Date();
  const diffTime = Math.abs(now - birth);
  const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24));
  
  const years = Math.floor(diffDays / 365);
  const months = Math.floor((diffDays % 365) / 30);
  const days = diffDays % 30;
  
  if (years > 0) {
    return months > 0 ? `${years}岁${months}个月` : `${years}岁`;
  } else if (months > 0) {
    return days > 0 ? `${months}个月${days}天` : `${months}个月`;
  } else {
    return `${days}天`;
  }
};

const debounce = (fn, delay = 500) => {
  let timer = null;
  return function (...args) {
    if (timer) clearTimeout(timer);
    timer = setTimeout(() => {
      fn.apply(this, args);
    }, delay);
  };
};

const throttle = (fn, delay = 500) => {
  let lastTime = 0;
  return function (...args) {
    const now = Date.now();
    if (now - lastTime >= delay) {
      fn.apply(this, args);
      lastTime = now;
    }
  };
};

module.exports = {
  formatTime,
  formatDate,
  formatDateTime,
  showSuccess,
  showError,
  showModal,
  getRecordTypeIcon,
  getRecordTypeText,
  calculateAge,
  debounce,
  throttle
};
