const LOCAL_ROOT = '/tmp/mount-test';
const LOCAL_ARCHIVE = `${LOCAL_ROOT}/archive.zip`;
const UNSUPPORTED_ROOT = 'https://example.com/';

// Roots naming a scheme a mount point does not support: each names a location off the local file
// system, or layers another provider over one that does. Schemes needing a server to answer (ftp,
// sftp, smb) are deliberately absent — a root using one fails for want of a server, whatever the
// scheme rule says, so a case on it would assert nothing.
const UNSUPPORTED_ROOTS = [
    'http://localhost:8080/',
    UNSUPPORTED_ROOT,
    `zip:file://${LOCAL_ARCHIVE}`,
    `jar:file://${LOCAL_ARCHIVE}`
];

const addVfs = (name: string, rootPath: string, errorPolicy?: 'all') => cy.apollo({
    mutationFile: 'addVfsJahiaPath.graphql',
    variables: {name, rootPath},
    errorPolicy
});

const setRootOf = (uuid: string, rootPath: string) => cy.apollo({
    mutationFile: 'modifyVfsJahiaPath.graphql',
    variables: {pathOrId: uuid, rootPath},
    errorPolicy: 'all'
});

const uuidOf = (response): string => response.data.admin.jahia.mountPoint.addVfs;

const expectAccepted = ({data}) => expect(data.admin.jahia.mountPoint.addVfs).to.not.be.empty;

const expectRefused = (rootPath: string) => ({errors, data}) => {
    expect(errors, `expected ${rootPath} to be refused`).to.not.be.empty;
    expect(errors.map(e => e.message).join(' '), 'the reported error should name the root').to.contain(rootPath);
    expect(data?.admin?.jahia?.mountPoint?.addVfs).to.be.oneOf([null, undefined]);
};

const expectRefusedChange = (rootPath: string) => ({errors}) => {
    expect(errors, `expected ${rootPath} to be refused`).to.not.be.empty;
    expect(errors.map(e => e.message).join(' '), 'the reported error should name the root').to.contain(rootPath);
};

const expectNamed = (name: string) => ({data}) => expect(data.jcr.nodeByPath.name).to.eq(name);

const expectRootPath = (rootPath: string) => ({data}) => {
    const property = data.admin.mountPoint.mountPoint.properties.find(p => p.key === 'j:rootPath');
    expect(property.value).to.eq(rootPath);
};

const readNode = (path: string) => cy.apollo({queryFile: 'getVfsNode.graphql', variables: {path}});

const readMountInfo = (name: string) => cy.apollo({queryFile: 'mountInfo.graphql', variables: {name}});

const storeRootOf = (uuid: string, rootPath: string) => cy.executeGroovy('setVfsRootPath.groovy', {
    '#uuid#': uuid,
    '#rootPath#': rootPath
});

const mount = (uuid: string) => cy.apollo({
    mutationFile: 'mountVfsJahiaPath.graphql',
    variables: {pathOrId: uuid},
    errorPolicy: 'all'
});

const expectNotMounted = ({data}) => expect(data.admin.mountPoint.mountPoint.mountStatus).to.not.eq('mounted');

describe('VFS mount point root path', () => {
    beforeEach(function () {
        cy.executeGroovy('cleanup.groovy');
        cy.executeGroovy('createDir.groovy');
        cy.executeGroovy('createVfsArchive.groovy', {'#path#': LOCAL_ARCHIVE});
    });

    afterEach(function () {
        cy.executeGroovy('cleanup.groovy');
    });

    it('accepts a supported root as a plain file system path', function () {
        addVfs('root-plain', LOCAL_ROOT).should(expectAccepted);
    });

    it('accepts a supported root with the file scheme', function () {
        addVfs('root-file-scheme', `file://${LOCAL_ROOT}`).should(expectAccepted);
    });

    it('serves the content of a supported root', function () {
        addVfs('root-served', LOCAL_ROOT);

        readNode('/mounts/root-served/images/tomcat.gif').should(expectNamed('tomcat.gif'));
    });

    UNSUPPORTED_ROOTS.forEach((rootPath, index) => {
        it(`refuses an unsupported root on creation: ${rootPath}`, function () {
            addVfs(`root-refused-${index}`, rootPath, 'all').should(expectRefused);
        });
    });

    it('refuses an unsupported root replacing a supported one', function () {
        addVfs('root-modified', LOCAL_ROOT)
            .then(uuidOf)
            .then(uuid => setRootOf(uuid, UNSUPPORTED_ROOT).should(expectRefusedChange(UNSUPPORTED_ROOT)));
    });

    it('leaves the other mount points serving when a stored root is not supported', function () {
        addVfs('root-alongside', LOCAL_ROOT);
        addVfs('root-stored', LOCAL_ROOT)
            .then(uuidOf)
            .then(uuid => storeRootOf(uuid, UNSUPPORTED_ROOT).then(() => mount(uuid)));

        readNode('/mounts/root-alongside/images/tomcat.gif').should(expectNamed('tomcat.gif'));
        readMountInfo('root-stored').should(expectNotMounted);
    });

    it('leaves the supported root in place when the change is refused', function () {
        addVfs('root-unchanged', LOCAL_ROOT)
            .then(uuidOf)
            .then(uuid => setRootOf(uuid, UNSUPPORTED_ROOT));

        readMountInfo('root-unchanged').should(expectRootPath(LOCAL_ROOT));
    });
});
