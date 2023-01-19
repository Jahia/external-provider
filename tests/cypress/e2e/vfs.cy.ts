const commonTests = (path => {
    it('should be able to get files in live', function () {
        cy.request(`/files/live${path}/images/tomcat.gif`);
    });

    it('should be able to get files in edit when logged', function () {
        cy.login();
        cy.request(`/files/default${path}/images/tomcat.gif`);
    });

    it('should be able to create folder with root', function () {
        cy.apollo({mutationFile: 'createFolder.graphql', variables: {path}});
        cy.executeGroovy('checkFile.groovy', {'#path#': '/tmp/mount-test/toto'}).should(r => {
            expect(r).to.eq('true');
        });
    });

    it('should be able to delete file', function () {
        cy.executeGroovy('checkFile.groovy', {'#path#': '/tmp/mount-test/index.html'}).should(r => {
            expect(r).to.eq('true');
        });
        cy.apollo({mutationFile: 'deleteNode.graphql', variables: {path: `${path}/index.html`}});
        cy.executeGroovy('checkFile.groovy', {'#path#': '/tmp/mount-test/index.html'}).should(r => {
            expect(r).to.eq('false');
        });
    });

    it('should be able to copy file from jcr to vfs', function () {
        cy.apollo({mutationFile: 'copyNode.graphql', variables: {path}});
        cy.executeGroovy('checkFile.groovy', {'#path#': '/tmp/mount-test/portrait-bill-taylor.jpg'}).should(r => {
            expect(r).to.eq('true');
        });
    });

    it('should be able to add tags mixin', function () {
        cy.apollo({mutationFile: 'addTags.graphql', variables: {path: `${path}/images/tomcat.gif`}});
        cy.apollo({queryFile: 'getTags.graphql', variables: {path: `${path}/images/tomcat.gif`}}).should(({data}) => {
            expect('jmix:tagged').to.be.oneOf(data.jcr.nodeByPath.mixinTypes.map(m => m.name));
            expect('tag1').to.be.oneOf(data.jcr.nodeByPath.property.values);
            expect('tag2').to.be.oneOf(data.jcr.nodeByPath.property.values);
        });
    });
});

describe('VFS mount operations tests', () => {
    beforeEach(function () {
        cy.executeGroovy('cleanup.groovy');
        cy.executeGroovy('createDir.groovy');
    });

    afterEach(function () {
        cy.apollo({mutationFile: 'deleteReference.graphql', errorPolicy: 'all'});
        cy.executeGroovy('cleanup.groovy');
    });

    describe('on a global mount point', () => {
        beforeEach(function () {
            cy.apollo({queryFile: 'mount.graphql'});
        });

        commonTests('/mounts/mount-test-mountPoint');

        it('should not be able to create folder with an editor', function () {
            cy.apolloClient({username: 'mathias', password: 'password'}).apollo({
                mutationFile: 'createFolder.graphql',
                variables: {path: '/mounts/mount-test-mountPoint'},
                errorPolicy: 'all'
            }).should(({errors}) => {
                expect('javax.jcr.AccessDeniedException: /').to.be.oneOf(errors.map(e => e.message));
            });
            cy.executeGroovy('checkFile.groovy', {'#path#': '/tmp/mount-test/toto'}).should(r => {
                expect(r).to.eq('false');
            });
        });

        it('should be available at new path after switching from global to local', function () {
            cy.apollo({mutationFile: 'moveToLocal.graphql'});
            cy.request('/files/live/sites/digitall/files/mount-test/images/tomcat.gif');
        });

        it('should keep references after switching from global to local', function () {
            cy.apollo({mutationFile: 'createReference.graphql'});
            cy.apollo({queryFile: 'getReference.graphql'}).should(({data}) => {
                expect(data.jcr.nodeByPath.property.refNode.path).eq('/mounts/mount-test-mountPoint/images/tomcat.gif');
            });
            cy.apollo({mutationFile: 'moveToLocal.graphql'});
            cy.apollo({queryFile: 'getReference.graphql'}).should(({data}) => {
                expect(data.jcr.nodeByPath.property.refNode.path).eq('/sites/digitall/files/mount-test/images/tomcat.gif');
            });
        });
    });

    describe('on a local mount point', () => {
        beforeEach(function () {
            cy.apollo({queryFile: 'mountLocal.graphql'});
        });

        commonTests('/sites/digitall/files/mount-test');

        it('should be able to create folder with an editor', function () {
            cy.apolloClient({username: 'mathias', password: 'password'}).apollo({
                mutationFile: 'createFolder.graphql',
                variables: {path: '/sites/digitall/files/mount-test'}
            });
            cy.executeGroovy('checkFile.groovy', {'#path#': '/tmp/mount-test/toto'}).should(r => {
                expect(r).to.eq('true');
            });
        });
    });
});
