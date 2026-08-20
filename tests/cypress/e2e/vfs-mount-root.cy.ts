const LOCAL_ROOT = '/tmp/mount-test';
const LOCAL_ARCHIVE = `${LOCAL_ROOT}/archive.zip`;

// Roots naming a scheme a mount point does not support: each names a location off the local file
// system, or layers another provider over one that does. Schemes needing a server to answer (ftp,
// sftp, smb) are deliberately absent — a root using one fails for want of a server, whatever the
// scheme rule says, so a case on it would assert nothing.
const UNSUPPORTED_ROOTS = [
    'http://localhost:8080/',
    'https://example.com/',
    `zip:file://${LOCAL_ARCHIVE}`,
    `jar:file://${LOCAL_ARCHIVE}`
];

const addVfs = (name: string, rootPath: string, errorPolicy?: 'all') => cy.apollo({
    mutationFile: 'addVfsJahiaPath.graphql',
    variables: {name, rootPath},
    errorPolicy
});

describe('VFS mount point root path', () => {
    beforeEach(function () {
        cy.executeGroovy('cleanup.groovy');
        cy.executeGroovy('createDir.groovy');
        cy.executeGroovy('createVfsArchive.groovy', {'#path#': LOCAL_ARCHIVE});
    });

    afterEach(function () {
        cy.executeGroovy('cleanup.groovy');
    });

    describe('a supported root', () => {
        it('is accepted as a plain file system path', function () {
            addVfs('root-plain', LOCAL_ROOT).should(({data}) => {
                expect(data.admin.jahia.mountPoint.addVfs).to.not.be.empty;
            });
        });

        it('is accepted with the file scheme', function () {
            addVfs('root-file-scheme', `file://${LOCAL_ROOT}`).should(({data}) => {
                expect(data.admin.jahia.mountPoint.addVfs).to.not.be.empty;
            });
        });

        it('serves the mounted content', function () {
            addVfs('root-served', LOCAL_ROOT);
            cy.apollo({
                queryFile: 'getVfsNode.graphql',
                variables: {path: '/mounts/root-served/images/tomcat.gif'}
            }).should(({data}) => {
                expect(data.jcr.nodeByPath.name).to.eq('tomcat.gif');
            });
        });
    });

    describe('an unsupported root', () => {
        UNSUPPORTED_ROOTS.forEach((rootPath, index) => {
            it(`is refused on creation: ${rootPath}`, function () {
                addVfs(`root-refused-${index}`, rootPath, 'all').should(({errors, data}) => {
                    expect(errors, `expected ${rootPath} to be refused`).to.not.be.empty;
                    expect(data?.admin?.jahia?.mountPoint?.addVfs).to.be.oneOf([null, undefined]);
                });
            });
        });

        it('is refused when it replaces a supported one', function () {
            addVfs('root-modified', LOCAL_ROOT).then(({data}) => {
                cy.apollo({
                    mutationFile: 'modifyVfsJahiaPath.graphql',
                    variables: {pathOrId: data.admin.jahia.mountPoint.addVfs, rootPath: 'https://example.com/'},
                    errorPolicy: 'all'
                }).should(({errors}) => {
                    expect(errors).to.not.be.empty;
                });
            });
        });

        it('leaves the supported root in place when the change is refused', function () {
            addVfs('root-unchanged', LOCAL_ROOT).then(({data}) => {
                cy.apollo({
                    mutationFile: 'modifyVfsJahiaPath.graphql',
                    variables: {pathOrId: data.admin.jahia.mountPoint.addVfs, rootPath: 'https://example.com/'},
                    errorPolicy: 'all'
                });
                cy.apollo({
                    queryFile: 'mountInfo.graphql',
                    variables: {name: 'root-unchanged'}
                }).should(({data: info}) => {
                    const rootPath = info.admin.mountPoint.mountPoint.properties
                        .find(p => p.key === 'j:rootPath').value;
                    expect(rootPath).to.eq(LOCAL_ROOT);
                });
            });
        });
    });
});
