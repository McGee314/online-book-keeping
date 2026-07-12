/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{vue,js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        primary: '#497e49',
        'primary-light': '#5a9a5a',
        'primary-dark': '#3a6a3a',
        success: '#67c23a',
        danger: '#f56c6c',
        warning: '#e6a23c',
        info: '#909399',
        background: '#f5f7fa',
        surface: '#ffffff',
        'text-primary': '#303133',
        'text-regular': '#606266',
        'text-secondary': '#909399',
      },
      fontFamily: {
        sans: ['Inter', 'system-ui', 'sans-serif'],
      },
    },
  },
  plugins: [],
  corePlugins: {
    preflight: false, // Don't conflict with Element Plus
  },
}