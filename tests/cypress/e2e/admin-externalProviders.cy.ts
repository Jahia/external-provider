describe('External providers admin panel', () => {
    const pageUrl = '/cms/adminframe/default/en/settings.external-provider.html?redirect=false';

    beforeEach(function () {
        cy.executeGroovy('cleanup.groovy');
        cy.executeGroovy('createDir.groovy');
        cy.login();
    });

    afterEach(function () {
        cy.executeGroovy('cleanup.groovy');
    });

    it('lists a mount point whose name contains quotes and an ampersand as typed', function () {
        const name = 'say "hi" & co';
        cy.apollo({queryFile: 'mountVfs.graphql', variables: {name}});
        cy.visit(pageUrl);
        cy.get('#providersTable').contains('td', `/mounts/${name}`);
        cy.get('#providersTable input[name="mountpoint"]').should('have.value', `/mounts/${name}`);
    });

    it('opens the data source information of a mount point whose name contains quotes', function () {
        const name = 'say "hi" & co';
        cy.apollo({queryFile: 'mountVfs.graphql', variables: {name}});
        cy.visit(pageUrl);
        cy.get('#providersTable button[name="_eventId_viewDataSource"]').click();
        cy.contains('h2', 'Data source information');
    });
});
