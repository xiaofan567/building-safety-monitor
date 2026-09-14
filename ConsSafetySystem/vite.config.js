import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'path'
import { fileURLToPath } from 'url'
import { dirname } from 'path'
const __filename = fileURLToPath(import.meta.url)
const __dirname = dirname(__filename)

// https://vite.dev/config/
export default defineConfig({
  plugins: [vue()],
  cacheDir: 'C:/Users/29500/.vite-cache-con',

  resolve: {
    alias: {
      '@': resolve(__dirname, 'src'),
      // 添加这一行，让Vue使用包含编译器的版本
      'vue': 'vue/dist/vue.esm-bundler.js'
    }
  },
  build: {
    sourcemap: false,
    outDir: 'C:/Users/29500/con-dist-out',
    emptyOutDir: true,
  },
  server: {
    fs: {
      // 允许访问项目根目录外的文件
      allow: ['..']
    }
  },
  optimizeDeps: {
    // 排除flv.min.js从依赖优化，避免sourcemap错误
    exclude: ['flvjs']
  }
})


