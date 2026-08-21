const LOCAL_ROOT = '/tmp/mount-test';
const LOCAL_GZIP = `${LOCAL_ROOT}/archive.gz`;

// A root that answers only to the configured set: the gzip provider is a scheme of its own, layered over a local
// file, so the probe reaches nothing off this machine. A root naming a remote scheme would answer the same
// question and open a real connection from wherever the suite runs. No other suite names this scheme, so
// widening it here cannot make a case elsewhere read against a set this one configured.
const CONFIGURED_ROOT = `gz:file://${LOCAL_GZIP}`;
const CONFIGURED_SCHEMES = 'file,gz';

// The same layered scheme over a root that is not on this machine. Naming the layer does not name what the layer
// reaches, so this stays refused and no connection is opened from wherever the suite runs.
const LAYERED_REMOTE_ROOT = 'gz:http://localhost:8080/';

// The configured set reaches the module asynchronously, so a probe is retried until it answers. Bounded by time
// rather than by attempts: what is being waited for is a delivery, not a number of round trips.
const ARRIVAL_TIMEOUT = 10000;
const ARRIVAL_INTERVAL = 250;

const addVfs = (name: string, rootPath: string, errorPolicy?: 'all') => cy.apollo({
    mutationFile: 'addVfsJahiaPath.graphql',
    variables: {name, rootPath},
    errorPolicy
});

const setAllowedSchemes = (value: string) => cy.apollo({
    mutationFile: 'setVfsAllowedSchemes.graphql',
    variables: {value}
});

// Also run before the suite, so a run that ended without reaching its own cleanup does not leave the
// set widened for whatever runs next
const clearAllowedSchemes = () => cy.apollo({
    mutationFile: 'clearVfsAllowedSchemes.graphql',
    errorPolicy: 'all'
});

const wasAccepted = response => !response.errors || response.errors.length === 0;

/**
 * Probes the configured root until it is answered as expected, so the test reads the set back rather than assume
 * it has arrived. Only the probe that is answered as expected creates a mount point, which the cleanup between
 * cases removes.
 */
const expectConfiguredRoot = (accepted: boolean) => {
    let attempt = 0;
    return cy.waitUntil(
        () => addVfs(`scheme-probe-${++attempt}`, CONFIGURED_ROOT, 'all')
            .then(response => wasAccepted(response) === accepted),
        {
            timeout: ARRIVAL_TIMEOUT,
            interval: ARRIVAL_INTERVAL,
            errorMsg: `${CONFIGURED_ROOT} is still ${accepted ? 'refused' : 'accepted'}`
        }
    );
};

describe('VFS mount point allowed root schemes', () => {
    before(function () {
        clearAllowedSchemes();
    });

    beforeEach(function () {
        cy.executeGroovy('cleanup.groovy');
        cy.executeGroovy('createDir.groovy');
        cy.executeGroovy('createVfsGzip.groovy', {'#path#': LOCAL_GZIP});
    });

    after(function () {
        clearAllowedSchemes();
        cy.executeGroovy('cleanup.groovy');
    });

    it('refuses a scheme the configuration does not name', function () {
        expectConfiguredRoot(false);
    });

    it('accepts a scheme the configuration names', function () {
        setAllowedSchemes(CONFIGURED_SCHEMES);

        expectConfiguredRoot(true);
    });

    it('keeps serving the local file system alongside a configured scheme', function () {
        setAllowedSchemes(CONFIGURED_SCHEMES);
        expectConfiguredRoot(true);

        addVfs('scheme-local', LOCAL_ROOT).should(response => {
            expect(response.data.admin.jahia.mountPoint.addVfs).to.not.be.empty;
        });
    });

    it('refuses a configured layered scheme over a root it does not name', function () {
        setAllowedSchemes(CONFIGURED_SCHEMES);
        expectConfiguredRoot(true);

        addVfs('scheme-layered', LAYERED_REMOTE_ROOT, 'all').should(response => {
            expect(response.errors, `expected ${LAYERED_REMOTE_ROOT} to be refused`).to.not.be.empty;
            expect(response.errors.map(e => e.message).join(' ')).to.contain(LAYERED_REMOTE_ROOT);
        });
    });

    it('refuses the scheme again once the configuration stops naming it', function () {
        setAllowedSchemes(CONFIGURED_SCHEMES);
        expectConfiguredRoot(true);

        clearAllowedSchemes();

        expectConfiguredRoot(false);
    });
});
