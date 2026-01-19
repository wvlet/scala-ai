<script setup>
import DefaultTheme from 'vitepress/theme'
import HomeHeroCode from './HomeHeroCode.vue'

const { Layout } = DefaultTheme
</script>

<template>
  <Layout>
    <template #home-hero-image>
      <HomeHeroCode />
    </template>
  </Layout>
</template>
