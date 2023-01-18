import gql from 'graphql-tag';

describe('VFS global mount tests', () => {
    beforeEach(function () {
        cy.executeGroovy('cleanup.groovy');
        cy.executeGroovy('createDir.groovy');
        cy.apollo({queryFile: 'mount.graphql'});
    });

    afterEach(function () {
        cy.executeGroovy('cleanup.groovy');
    });

    it('should be able to get files in live', function () {
        cy.request('/files/live/mounts/mount-test-mountPoint/images/tomcat.gif');
    });

    it('should be able to get files in edit when logged', function () {
        cy.login();
        cy.request('/files/default/mounts/mount-test-mountPoint/images/tomcat.gif');
    });

    it('should be displayed in admin', function () {
        cy.login();
        cy.visit('http://localhost:8080/cms/adminframe/default/en/settings.manageMountPoints.html?redirect=false');
        cy.contains('mount-test-mountPoint');
    });

    it('should be able to create folder with root', function () {
        cy.apollo({
            mutation: gql`mutation {
                jcr {
                    mutateNode(pathOrId: "/mounts/mount-test-mountPoint") {
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

    it('should not be able to create folder with an editor', function () {
        cy.apolloClient({username: 'mathias', password: 'password'}).apollo({
            mutation: gql`mutation {
                jcr {
                    mutateNode(pathOrId: "/mounts/mount-test-mountPoint") {
                        addChild(name: "toto", primaryNodeType: "jnt:folder") {
                            uuid
                        }
                    }
                }
            }`,
            errorPolicy: 'all'
        }).should(({errors}) => {
            expect('javax.jcr.AccessDeniedException: /').to.be.oneOf(errors.map(e => e.message));
        });
        cy.executeGroovy('checkFile.groovy', {'#path#': '/tmp/mount-test/toto'}).should(r => {
            expect(r).to.eq('false');
        });
    });

    it('should be able to delete file', function () {
        cy.executeGroovy('checkFile.groovy', {'#path#': '/tmp/mount-test/index.html'}).should(r => {
            expect(r).to.eq('true');
        });
        cy.apollo({
            mutation: gql`mutation {
                jcr {
                    mutateNode(pathOrId: "/mounts/mount-test-mountPoint/index.html") {
                        delete
                    }
                }
            }`
        });
        cy.executeGroovy('checkFile.groovy', {'#path#': '/tmp/mount-test/index.html'}).should(r => {
            expect(r).to.eq('false');
        });
    });

    it('should be able to copy file from jcr to vfs', function () {
        cy.apollo({
            mutation: gql`mutation {
                jcr {
                    copyNode(pathOrId:"/sites/digitall/files/images/people/portrait-bill-taylor.jpg", destParentPathOrId:"/mounts/mount-test-mountPoint") {
                        uuid
                    }
                }
            }`
        });
        cy.executeGroovy('checkFile.groovy', {'#path#': '/tmp/mount-test/portrait-bill-taylor.jpg'}).should(r => {
            expect(r).to.eq('true');
        });
    });

    it('should be able to add tags mixin', function () {
        cy.apollo({
            mutation: gql`mutation m {
                jcr {
                    mutateNode(pathOrId:"/mounts/mount-test-mountPoint/images/tomcat.gif") {
                        addMixins(mixins:["jmix:tagged"])
                        mutateProperty(name:"j:tagList") {
                            setValues(values:["tag1", "tag2"])
                        }
                    }
                }
            }`
        });
        cy.apollo({
            query: gql`query q {
                jcr {
                    nodeByPath(path:"/mounts/mount-test-mountPoint/images/tomcat.gif"){
                        mixinTypes {
                            name
                        }
                        property(name:"j:tagList") {
                            values
                        }
                    }
                }
            }`
        }).should(({data}) => {
            expect('jmix:tagged').to.be.oneOf(data.jcr.nodeByPath.mixinTypes.map(m => m.name));
            expect('tag1').to.be.oneOf(data.jcr.nodeByPath.property.values);
            expect('tag2').to.be.oneOf(data.jcr.nodeByPath.property.values);
        });
    });
});
