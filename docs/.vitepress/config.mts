import { defineConfig } from 'vitepress'

export default defineConfig({
  title: 'wvlet/uni',
  description: 'Scala 3 Unified Utility Library',

  cleanUrls: true,

  themeConfig: {
    nav: [
      { text: 'Guide', link: '/guide/' },
      { text: 'Core', link: '/core/' },
      { text: 'Control', link: '/control/' },
      { text: 'HTTP', link: '/http/' },
      { text: 'Rx', link: '/rx/' },
      { text: 'CLI', link: '/cli/' },
      { text: 'Agent', link: '/agent/' },
      { text: 'Testing', link: '/testing/' },
      {
        text: 'GitHub',
        link: 'https://github.com/wvlet/uni'
      }
    ],

    sidebar: {
      '/guide/': [
        {
          text: 'Guide',
          items: [
            { text: 'Introduction', link: '/guide/' },
            { text: 'Installation', link: '/guide/installation' },
            { text: 'Design Principles', link: '/guide/principles' }
          ]
        }
      ],
      '/core/': [
        {
          text: 'Core Utilities',
          items: [
            { text: 'Overview', link: '/core/' },
            { text: 'Dependency Injection', link: '/core/design' },
            { text: 'Logging', link: '/core/logging' },
            { text: 'JSON Processing', link: '/core/json' },
            { text: 'MessagePack', link: '/core/msgpack' },
            { text: 'Type Introspection', link: '/core/surface' }
          ]
        }
      ],
      '/control/': [
        {
          text: 'Control Flow',
          items: [
            { text: 'Overview', link: '/control/' },
            { text: 'Retry Logic', link: '/control/retry' },
            { text: 'Circuit Breaker', link: '/control/circuit-breaker' },
            { text: 'Caching', link: '/control/cache' },
            { text: 'Resource Management', link: '/control/resource' }
          ]
        }
      ],
      '/http/': [
        {
          text: 'HTTP Framework',
          items: [
            { text: 'Overview', link: '/http/' },
            { text: 'HTTP Client', link: '/http/client' },
            { text: 'Retry Strategies', link: '/http/retry' }
          ]
        }
      ],
      '/rx/': [
        {
          text: 'Reactive Streams',
          items: [
            { text: 'Overview', link: '/rx/' },
            { text: 'Basics', link: '/rx/basics' },
            { text: 'Operators', link: '/rx/operators' }
          ]
        }
      ],
      '/cli/': [
        {
          text: 'CLI Utilities',
          items: [
            { text: 'Overview', link: '/cli/' },
            { text: 'Terminal Styling', link: '/cli/chalk' },
            { text: 'Progress Indicators', link: '/cli/progress' },
            { text: 'Command Launcher', link: '/cli/launcher' }
          ]
        }
      ],
      '/agent/': [
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
      '/testing/': [
        {
          text: 'Testing',
          items: [
            { text: 'Overview', link: '/testing/' },
            { text: 'Assertions', link: '/testing/assertions' }
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
