import DefaultTheme from 'vitepress/theme'
import CustomHome from './CustomHome.vue'
import './custom.css'

export default {
  extends: DefaultTheme,
  Layout: CustomHome
}
