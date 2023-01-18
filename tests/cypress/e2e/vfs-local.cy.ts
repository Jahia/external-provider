import gql from 'graphql-tag';

describe('VFS local mount tests', () => {
    beforeEach(function () {
        cy.executeGroovy('cleanup.groovy');
        cy.executeGroovy('createDir.groovy');
        cy.apollo({queryFile: 'mountLocal.graphql'});
    });

    afterEach(function () {
        cy.executeGroovy('cleanup.groovy');
    });

    it('should be able to get files in live', function () {
        cy.request('/files/live/sites/digitall/files/mount-test/images/tomcat.gif');
    });

    it('should be able to get files in edit when logged', function () {
        cy.login('mathias', 'password');
        cy.request('/files/default/sites/digitall/files/mount-test/images/tomcat.gif');
    });

    it('should be displayed in admin', function () {
        cy.login();
        cy.visit('http://localhost:8080/cms/adminframe/default/en/settings.manageMountPoints.html?redirect=false');
        cy.contains('/sites/digitall/files/mount-test');
    });

    it('should be able to create folder with an editor', function () {
        cy.apolloClient({username: 'mathias', password: 'password'}).apollo({
            mutation: gql`mutation {
                jcr {
                    mutateNode(pathOrId: "/sites/digitall/files/mount-test") {
                        addChild(name: "toto", primaryNodeType: "jnt:folder") {
                            uuid
                        }
                    }
                }
            }`
        });
        cy.executeGroovy('checkFile.groovy', {'#path#': '/tmp/mount-test/toto'}).should(r => {
            expect(r).to.eq('true');
        });
    });
});
