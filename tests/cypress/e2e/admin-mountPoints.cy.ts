describe('VFS admin panel mount tests', () => {
    beforeEach(function () {
        cy.executeGroovy('cleanup.groovy');
        cy.executeGroovy('createDir.groovy');
        cy.login();
        cy.visit('http://localhost:8080/cms/adminframe/default/en/settings.manageMountPoints.html?redirect=false');
    });

    afterEach(function () {
        cy.executeGroovy('cleanup.groovy');
    });

    it('Create global', function () {
        cy.get('button[data-sel-role="addMountPoint"]').click();
        cy.get('input[name="name"]').type('mount-test');
        cy.get('input[name="root"]').type('/tmp/mount-test');
        cy.get('button[name="_eventId_save"]').click();
        cy.contains('/mounts/mount-test');
    });

    it('Create local', function () {
        cy.get('button[data-sel-role="addMountPoint"]').click();
        cy.get('input[name="name"]').type('mount-test');
        cy.get('input[name="root"]').type('/tmp/mount-test');
        cy.contains('Select a target local mount point').click();
        cy.get('input[value="/sites/digitall/files"]').click();
        cy.get('button[name="_eventId_save"]').click();
        cy.contains('/sites/digitall/files/mount-test');
    });
});
