const screens = [
    {variant: 'default', url: '/cms/adminframe/default/en/settings.external-provider.html?redirect=false'},
    {variant: 'anthracite', url: '/cms/adminframe/default/en/settings.external-provider-jahia-anthracite.html?redirect=false'}
];

describe('External providers admin panel', () => {
    const name = 'say "hi" & co';
    const mountPath = `/mounts/${name}`;

    beforeEach(function () {
        cy.executeGroovy('cleanup.groovy');
        cy.executeGroovy('createDir.groovy');
        cy.login();
        cy.apollo({queryFile: 'mountVfs.graphql', variables: {name}});
    });

    afterEach(function () {
        cy.executeGroovy('cleanup.groovy');
    });

    screens.forEach(({variant, url}) => {
        describe(`${variant} template`, () => {
            beforeEach(function () {
                cy.visit(url);
            });

            it('lists a mount point whose name contains quotes and an ampersand as typed', function () {
                cy.contains('#providersTable tr', mountPath).within(() => {
                    cy.contains('td', mountPath);
                    cy.get('input[name="mountpoint"]').should('have.value', mountPath);
                });
            });

            it('opens the data source information of a mount point whose name contains quotes', function () {
                cy.contains('#providersTable tr', mountPath).find('button[name="_eventId_viewDataSource"]').click();
                cy.contains('h2', 'Data source information');
            });
        });
    });
});
