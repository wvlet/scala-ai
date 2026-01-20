import { defineConfig } from 'vitepress'

export default defineConfig({
  title: 'Uni',
  description: 'Essential Scala Utilities - Refined for Scala 3 with minimal dependencies',

  base: '/uni/',
  cleanUrls: true,

  markdown: {
    theme: {
      light: 'nord',
      dark: 'nord'
    }
  },

  head: [
    ['link', { rel: 'icon', href: '/favicon.ico' }]
  ],

  themeConfig: {
    logo: '/logo.png',
    nav: [
      { text: 'Guide', link: '/guide/' },
      { text: 'Modules', link: '/core/' },
      {
        text: 'GitHub',
        link: 'https://github.com/wvlet/uni'
      }
    ],

    sidebar: {
      '/guide/': [
        {
          text: 'Getting Started',
          items: [
            { text: 'Introduction', link: '/guide/' },
            { text: 'Installation', link: '/guide/installation' },
            { text: 'Design Principles', link: '/guide/principles' }
          ]
        },
        {
          text: 'Core',
          items: [
            { text: 'Overview', link: '/core/' },
            { text: 'Design', link: '/core/design' },
            { text: 'UniTest', link: '/core/unitest' },
            { text: 'Logging', link: '/core/logging' },
            { text: 'JSON Processing', link: '/core/json' },
            { text: 'MessagePack', link: '/core/msgpack' },
            { text: 'Type Introspection', link: '/core/surface' }
          ]
        },
        {
          text: 'Control Flow',
          items: [
            { text: 'Overview', link: '/control/' },
            { text: 'Retry Logic', link: '/control/retry' },
            { text: 'Circuit Breaker', link: '/control/circuit-breaker' },
            { text: 'Caching', link: '/control/cache' },
            { text: 'Resource Management', link: '/control/resource' }
          ]
        },
        {
          text: 'HTTP',
          items: [
            { text: 'Overview', link: '/http/' },
            { text: 'HTTP Client', link: '/http/client' },
            { text: 'Retry Strategies', link: '/http/retry' }
          ]
        },
        {
          text: 'Reactive Streams',
          items: [
            { text: 'Overview', link: '/rx/' },
            { text: 'Basics', link: '/rx/basics' },
            { text: 'Operators', link: '/rx/operators' }
          ]
        },
        {
          text: 'CLI',
          items: [
            { text: 'Overview', link: '/cli/' },
            { text: 'Terminal Styling', link: '/cli/tint' },
            { text: 'Progress Indicators', link: '/cli/progress' },
            { text: 'Command Launcher', link: '/cli/launcher' }
          ]
        },
        {
          text: 'Agent Framework',
          items: [
            { text: 'Overview', link: '/agent/' },
            { text: 'LLM Agent', link: '/agent/llm-agent' },
            { text: 'Chat Sessions', link: '/agent/chat-session' },
            { text: 'Tool Integration', link: '/agent/tools' },
            { text: 'AWS Bedrock', link: '/agent/bedrock' }
          ]
        }
      ],
      '/': [
        {
          text: 'Getting Started',
          items: [
            { text: 'Introduction', link: '/guide/' },
            { text: 'Installation', link: '/guide/installation' },
            { text: 'Design Principles', link: '/guide/principles' }
          ]
        },
        {
          text: 'Core',
          items: [
            { text: 'Overview', link: '/core/' },
            { text: 'Design', link: '/core/design' },
            { text: 'UniTest', link: '/core/unitest' },
            { text: 'Logging', link: '/core/logging' },
            { text: 'JSON Processing', link: '/core/json' },
            { text: 'MessagePack', link: '/core/msgpack' },
            { text: 'Type Introspection', link: '/core/surface' }
          ]
        },
        {
          text: 'Control Flow',
          items: [
            { text: 'Overview', link: '/control/' },
            { text: 'Retry Logic', link: '/control/retry' },
            { text: 'Circuit Breaker', link: '/control/circuit-breaker' },
            { text: 'Caching', link: '/control/cache' },
            { text: 'Resource Management', link: '/control/resource' }
          ]
        },
        {
          text: 'HTTP',
          items: [
            { text: 'Overview', link: '/http/' },
            { text: 'HTTP Client', link: '/http/client' },
            { text: 'Retry Strategies', link: '/http/retry' }
          ]
        },
        {
          text: 'Reactive Streams',
          items: [
            { text: 'Overview', link: '/rx/' },
            { text: 'Basics', link: '/rx/basics' },
            { text: 'Operators', link: '/rx/operators' }
          ]
        },
        {
          text: 'CLI',
          items: [
            { text: 'Overview', link: '/cli/' },
            { text: 'Terminal Styling', link: '/cli/tint' },
            { text: 'Progress Indicators', link: '/cli/progress' },
            { text: 'Command Launcher', link: '/cli/launcher' }
          ]
        },
        {
          text: 'Agent Framework',
          items: [
            { text: 'Overview', link: '/agent/' },
            { text: 'LLM Agent', link: '/agent/llm-agent' },
            { text: 'Chat Sessions', link: '/agent/chat-session' },
            { text: 'Tool Integration', link: '/agent/tools' },
            { text: 'AWS Bedrock', link: '/agent/bedrock' }
          ]
        }
      ]
    },

    socialLinks: [
      { icon: 'github', link: 'https://github.com/wvlet/uni' }
    ],

    search: {
      provider: 'local'
    },

    footer: {
      message: 'Released under the Apache 2.0 License.',
      copyright: 'Copyright © 2025 wvlet'
    }
  }
})
