describe('GraphQL endpoint tests', () => {
    before(function () {
        cy.executeGroovy('cleanup.groovy');
        cy.executeGroovy('createDir.groovy');
    });

    after(function () {
        cy.executeGroovy('cleanup.groovy');
    });

    describe('Simple mount point', () => {
        it('create simple mount point', () => {
            cy.apollo({
                mutationFile: 'mountSimple.graphql',
                variables: {name: 'simple'}
            }).then(resp => {
                expect(resp.data.admin.mountPoint.add).to.not.be.empty;
            });
        });

        it('unmount mount point', () => {
            cy.apollo({
                queryFile: 'unmount.graphql',
                variables: {pathOrId: '/mounts/simple-mount'}
            }).then(resp => {
                expect(resp.data.admin.mountPoint.unmount).to.be.true;
            });

            cy.apollo({
                queryFile: 'mountInfo.graphql',
                variables: {name: 'simple'}
            }).then(resp => {
                const mountPoint = resp.data.admin.mountPoint.mountPoint;
                expect(mountPoint).to.not.be.empty;
                expect(mountPoint.mountStatus).to.equal('unmounted');
            });
        });

        it('remount mount point', () => {
            cy.apollo({
                queryFile: 'mounted.graphql',
                variables: {pathOrId: '/mounts/simple-mount'}
            }).then(resp => {
                expect(resp.data.admin.mountPoint.mount).to.be.true;
            });

            cy.apollo({
                queryFile: 'mountInfo.graphql',
                variables: {name: 'simple'}
            }).then(resp => {
                const mountPoint = resp.data.admin.mountPoint.mountPoint;
                expect(mountPoint).to.not.be.empty;
                expect(mountPoint.mountStatus).to.equal('mounted');
            });
        });
    });

    describe('VFS mount point', () => {
        const mountInfo = {name: 'my-vfs-mount', rootPath: '/tmp/mount-test'};

        it('create VFS mount point', () => {
            cy.apollo({
                mutationFile: 'mountVfs.graphql',
                variables: {...mountInfo, mountPointRefPath: '/sites/digitall/files'}
            }).then(resp => {
                expect(resp.data.admin.mountPoint.addVfs).to.not.be.empty;
            });
        });

        it('create duplicate VFS mount point name with local', () => {
            cy.apollo({
                mutationFile: 'mountVfs.graphql',
                variables: mountInfo
            }).then(resp => {
                expect(resp.data.admin.mountPoint.addVfs).to.not.be.empty;
            });
        });

        it('get all mount points', () => {
            cy.apollo({
                queryFile: 'mountInfos.graphql'
            }).then(resp => {
                expect(resp.data.admin.mountPoint.mountPoints).to.not.be.empty;
                expect(resp.data.admin.mountPoint.mountPoints.length).to.equal(2, 'Duplicate mount point nodes');
            });
        });

        it('get mount point information', () => {
            cy.apollo({
                queryFile: 'mountInfo.graphql',
                variables: {name: 'my-vfs-mount'}
            }).then(resp => {
                const mountPoint = resp.data.admin.mountPoint.mountPoint;
                expect(mountPoint).to.not.be.empty;
                expect(mountPoint.mountName).to.equal('my-vfs-mount');
                expect(mountPoint.mountStatus).to.equal('mounted');
                expect(mountPoint.mountPointRefPath).to.equal('/sites/digitall/files/my-vfs-mount');
                expect(mountPoint.properties.find(p => p.key === 'j:rootPath').value).to.equal('/tmp/mount-test');
            });
        });

        it('modify mount point', () => {
            cy.apollo({
                queryFile: 'mountInfo.graphql',
                variables: {name: 'my-vfs-mount-1'}
            }).its('data.admin.mountPoint.mountPoint.uuid').as('nodeId');

            cy.get('@nodeId').then(nodeId => {
                cy.apollo({
                    mutationFile: 'modifyVfs.graphql',
                    variables: {
                        pathOrId: nodeId,
                        name: 'new-mount-name'
                    }
                }).then(resp => {
                    expect(resp.data.admin.mountPoint.modifyVfs).to.be.true;
                });
            });
        });

        it('get modified mount point information', () => {
            cy.apollo({
                queryFile: 'mountInfo.graphql',
                variables: {name: 'new-mount-name'}
            }).then(resp => {
                const mountPoint = resp.data.admin.mountPoint.mountPoint;
                expect(mountPoint).to.not.be.empty;
                expect(mountPoint.mountName).to.equal('new-mount-name');
                expect(mountPoint.mountStatus).to.equal('mounted');
                expect(mountPoint.mountPointRefPath).to.equal('/mounts/new-mount-name');
                expect(mountPoint.properties.find(p => p.key === 'j:rootPath').value).to.equal('/tmp/mount-test');
            });
        });

        it('unmount mount point', () => {
            cy.apollo({
                queryFile: 'unmount.graphql',
                variables: {pathOrId: '/mounts/new-mount-name-mount'}
            }).then(resp => {
                expect(resp.data.admin.mountPoint.unmount).to.be.true;
            });

            cy.apollo({
                queryFile: 'mountInfo.graphql',
                variables: {name: 'new-mount-name'}
            }).then(resp => {
                const mountPoint = resp.data.admin.mountPoint.mountPoint;
                expect(mountPoint).to.not.be.empty;
                expect(mountPoint.mountStatus).to.equal('unmounted');
            });
        });

        it('remount mount point', () => {
            cy.apollo({
                queryFile: 'mounted.graphql',
                variables: {pathOrId: '/mounts/new-mount-name-mount'}
            }).then(resp => {
                expect(resp.data.admin.mountPoint.mount).to.be.true;
            });

            cy.apollo({
                queryFile: 'mountInfo.graphql',
                variables: {name: 'new-mount-name'}
            }).then(resp => {
                const mountPoint = resp.data.admin.mountPoint.mountPoint;
                expect(mountPoint).to.not.be.empty;
                expect(mountPoint.mountStatus).to.equal('mounted');
            });
        });
    });
});
