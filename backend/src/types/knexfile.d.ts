declare module '@knexfile' {
  const config: { [env: string]: import('knex').Knex.Config };
  export default config;
}