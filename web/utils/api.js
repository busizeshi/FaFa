const request = require('./request');

const petApi = {
  getList: (userId) => request.get(`/pets?userId=${userId}`),
  getDetail: (id) => request.get(`/pets/${id}`),
  create: (data) => request.post('/pets', data),
  update: (id, data) => request.put(`/pets/${id}`, data),
  delete: (id) => request.delete(`/pets/${id}`)
};

const feedingApi = {
  getList: (petId, query = {}) => {
    const queryStr = Object.keys(query)
      .map(key => `${key}=${query[key]}`)
      .join('&');
    return request.get(`/records/feeding?petId=${petId}&${queryStr}`);
  },
  getDetail: (id) => request.get(`/records/feeding/${id}`),
  create: (data) => request.post('/records/feeding', data),
  update: (id, data) => request.put(`/records/feeding/${id}`, data),
  delete: (id) => request.delete(`/records/feeding/${id}`)
};

const waterApi = {
  getList: (petId, query = {}) => {
    const queryStr = Object.keys(query)
      .map(key => `${key}=${query[key]}`)
      .join('&');
    return request.get(`/records/water?petId=${petId}&${queryStr}`);
  },
  getDetail: (id) => request.get(`/records/water/${id}`),
  create: (data) => request.post('/records/water', data),
  update: (id, data) => request.put(`/records/water/${id}`, data),
  delete: (id) => request.delete(`/records/water/${id}`)
};

const weightApi = {
  getList: (petId, query = {}) => {
    const queryStr = Object.keys(query)
      .map(key => `${key}=${query[key]}`)
      .join('&');
    return request.get(`/records/weight?petId=${petId}&${queryStr}`);
  },
  getDetail: (id) => request.get(`/records/weight/${id}`),
  create: (data) => request.post('/records/weight', data),
  update: (id, data) => request.put(`/records/weight/${id}`, data),
  delete: (id) => request.delete(`/records/weight/${id}`),
  getChart: (petId, period) => request.get(`/records/weight/chart?petId=${petId}&period=${period}`)
};

const medicalApi = {
  getList: (petId, query = {}) => {
    const queryStr = Object.keys(query)
      .map(key => `${key}=${query[key]}`)
      .join('&');
    return request.get(`/records/medical?petId=${petId}&${queryStr}`);
  },
  getDetail: (id) => request.get(`/records/medical/${id}`),
  create: (data) => request.post('/records/medical', data),
  update: (id, data) => request.put(`/records/medical/${id}`, data),
  delete: (id) => request.delete(`/records/medical/${id}`)
};

const vaccinationApi = {
  getList: (petId) => request.get(`/records/vaccination?petId=${petId}`),
  getDetail: (id) => request.get(`/records/vaccination/${id}`),
  create: (data) => request.post('/records/vaccination', data),
  update: (id, data) => request.put(`/records/vaccination/${id}`, data),
  delete: (id) => request.delete(`/records/vaccination/${id}`)
};

const reminderApi = {
  getList: (petId) => request.get(`/reminders?petId=${petId}`),
  getDetail: (id) => request.get(`/reminders/${id}`),
  create: (data) => request.post('/reminders', data),
  update: (id, data) => request.put(`/reminders/${id}`, data),
  delete: (id) => request.delete(`/reminders/${id}`),
  toggle: (id, enabled) => request.put(`/reminders/${id}/toggle`, { enabled })
};

const timelineApi = {
  getList: (petId, query = {}) => {
    const queryStr = Object.keys(query)
      .map(key => `${key}=${query[key]}`)
      .join('&');
    return request.get(`/timeline?petId=${petId}&${queryStr}`);
  }
};

const userApi = {
  login: (data) => request.post('/auth/login', data),
  register: (data) => request.post('/auth/register', data),
  getUserInfo: () => request.get('/user/info'),
  updateUserInfo: (data) => request.put('/user/info', data)
};

module.exports = {
  petApi,
  feedingApi,
  waterApi,
  weightApi,
  medicalApi,
  vaccinationApi,
  reminderApi,
  timelineApi,
  userApi
};
