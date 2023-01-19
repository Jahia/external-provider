describe('VFS admin panel mount tests', () => {
    beforeEach(function () {
        cy.executeGroovy('cleanup.groovy');
        cy.executeGroovy('createDir.groovy');
        cy.login();
    });

    afterEach(function () {
        cy.executeGroovy('cleanup.groovy');
    });

    describe('without an existing mount point', () => {
        beforeEach(function () {
            cy.visit('/cms/adminframe/default/en/settings.manageMountPoints.html?redirect=false');
        });

        it('create a global mount point', function () {
            cy.visit('/cms/adminframe/default/en/settings.manageMountPoints.html?redirect=false');
            cy.get('button[data-sel-role="addMountPoint"]').click();
            cy.get('input[name="name"]').type('mount-test');
            cy.get('input[name="root"]').type('/tmp/mount-test');
            cy.get('button[name="_eventId_save"]').click();
            cy.contains('Mounted');
            cy.contains('/mounts/mount-test');
        });
        it('can create a local mountpoint', function () {
            cy.visit('/cms/adminframe/default/en/settings.manageMountPoints.html?redirect=false');
            cy.get('button[data-sel-role="addMountPoint"]').click();
            cy.get('input[name="name"]').type('mount-test');
            cy.get('input[name="root"]').type('/tmp/mount-test');
            cy.contains('Select a target local mount point').click();
            cy.get('input[value="/sites/digitall/files"]').click();
            cy.get('button[name="_eventId_save"]').click();
            cy.contains('Mounted');
            cy.contains('/sites/digitall/files/mount-test');
        });
    });

    describe('with an existing mount point', () => {
        beforeEach(function () {
            cy.apollo({queryFile: 'mount.graphql'});
            cy.visit('/cms/adminframe/default/en/settings.manageMountPoints.html?redirect=false');
        });

        it('should be displayed in admin', function () {
            cy.contains('/mounts/mount-test');
        });

        it('can delete a mount point', function () {
            cy.get('button[data-original-title="Delete"]').click();
            cy.on('window:confirm', () => true);
            cy.contains('/mounts/mount-test').should('not.exist');
        });

        it('can unmount/mount a mount point', function () {
            cy.get('button[data-original-title="Unmount"]').click();
            cy.contains('Unmounted');
            cy.contains('/mounts/mount-test');
            cy.get('button[data-original-title="Mount"]').click();
            cy.contains('Mounted');
        });
    });
});
