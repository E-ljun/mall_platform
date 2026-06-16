export default {
  plugins: {
    autoprefixer: {
      overrideBrowserslist: [
        'Chrome >= 64',
        'Edge >= 79',
        'Firefox >= 68',
        'Safari >= 12',
      ],
      grid: 'autoplace',
      flexbox: 'no-2009',
    },
  },
}
