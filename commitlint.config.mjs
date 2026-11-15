const PIES_PROHIBIDOS = /^(co-authored-by:|.*generad[oa] con|.*generated with)/im;

export default {
  extends: ['@commitlint/config-conventional'],
  plugins: [
    {
      rules: {
        'sin-coautoria': ({ raw }) => [
          !PIES_PROHIBIDOS.test(raw),
          'no se permiten líneas de coautoría ni pies del tipo "generado con"',
        ],
      },
    },
  ],
  rules: {
    'scope-enum': [2, 'always', ['dominio', 'datos', 'ui', 'sync', 'adr', 'ci', 'repo', 'deps']],
    'header-max-length': [2, 'always', 100],
    'sin-coautoria': [2, 'always'],
  },
};
