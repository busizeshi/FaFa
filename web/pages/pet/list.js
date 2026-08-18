const app = getApp();
const { petApi } = require('../../utils/api');
const util = require('../../utils/util');

Page({
  data: {
    pets: [],
    loading: true
  },

  onLoad() {
    this.loadPets();
  },

  async loadPets() {
    try {
      this.setData({ loading: true });
      const pets = await petApi.getList();
      this.setData({ 
        pets: pets.sort((a, b) => a.sortOrder - b.sortOrder),
        loading: false 
      });
    } catch (error) {
      console.error('加载宠物列表失败:', error);
      this.setData({ loading: false });
    }
  },

  onPetTap(e) {
    const { id } = e.currentTarget.dataset;
    wx.navigateTo({ url: `/pages/pet/profile?id=${id}` });
  },

  onSelectPet(e) {
    const { id } = e.currentTarget.dataset;
    app.setCurrentPet(id);
    util.showSuccess('已切换宠物');
    setTimeout(() => {
      wx.navigateBack();
    }, 800);
  },

  onEditPet(e) {
    const { id } = e.currentTarget.dataset;
    wx.navigateTo({ url: `/pages/pet/edit?id=${id}` });
  },

  async onDeletePet(e) {
    const { id, name } = e.currentTarget.dataset;
    
    const confirm = await util.showModal('确认删除', `确定要删除${name}的档案吗？此操作不可恢复。`);
    if (!confirm) return;

    try {
      await petApi.delete(id);
      util.showSuccess('删除成功');
      
      if (app.getCurrentPet() === id) {
        const remainingPets = this.data.pets.filter(p => p.id !== id);
        if (remainingPets.length > 0) {
          app.setCurrentPet(remainingPets[0].id);
        } else {
          app.setCurrentPet(null);
        }
      }
      
      this.loadPets();
    } catch (error) {
      util.showError('删除失败');
    }
  },

  onAddPet() {
    wx.navigateTo({ url: '/pages/pet/create' });
  },

  onDragStart(e) {
    const { id, index } = e.currentTarget.dataset;
    this.dragData = { id, index };
  },

  onDragEnd() {
    this.dragData = null;
  },

  async onDrop(e) {
    if (!this.dragData) return;
    
    const { index: targetIndex } = e.currentTarget.dataset;
    const { index: sourceIndex } = this.dragData;
    
    if (sourceIndex === targetIndex) return;

    const pets = [...this.data.pets];
    const [draggedPet] = pets.splice(sourceIndex, 1);
    pets.splice(targetIndex, 0, draggedPet);

    const sortData = pets.map((pet, idx) => ({
      id: pet.id,
      sortOrder: idx
    }));

    try {
      await petApi.updateSort(sortData);
      this.setData({ pets });
    } catch (error) {
      util.showError('排序失败');
    }
  },

  getAge: util.getAge,
  getSpeciesText: util.getSpeciesText,
  getGenderText: util.getGenderText,
  formatNumber: util.formatNumber
});
