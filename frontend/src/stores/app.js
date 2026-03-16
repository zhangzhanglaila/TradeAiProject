import { defineStore } from 'pinia'

export const useAppStore = defineStore('app', {
  state: () => ({
    collapsed: false,
    breadcrumbs: [],
    tagsView: []
  }),

  actions: {
    toggleCollapsed() {
      this.collapsed = !this.collapsed
    },

    setBreadcrumbs(breadcrumbs) {
      this.breadcrumbs = breadcrumbs
    },

    addTagView(tag) {
      const exists = this.tagsView.find(t => t.path === tag.path)
      if (!exists) {
        this.tagsView.push(tag)
      }
    },

    removeTagView(path) {
      const index = this.tagsView.findIndex(t => t.path === path)
      if (index > -1) {
        this.tagsView.splice(index, 1)
      }
    }
  }
})
