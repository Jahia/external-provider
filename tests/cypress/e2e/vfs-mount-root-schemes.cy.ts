const LOCAL_ROOT = '/tmp/mount-test';

// A root reaching off the local file system, so it answers only to the configured set
const CONFIGURED_ROOT = 'https://example.com/';
const CONFIGURED_SCHEMES = 'file,https';

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
 * The configured set reaches the module through the configuration service, so read the answer back rather than
 * assume it has arrived. Each attempt is a round trip, which is the wait.
 */
const expectConfiguredRoot = (accepted: boolean, attempt = 1) =>
    addVfs(`scheme-probe-${attempt}`, CONFIGURED_ROOT, 'all').then(response => {
        if (wasAccepted(response) === accepted) {
            return cy.wrap(response);
        }

        expect(attempt, `${CONFIGURED_ROOT} is still ${accepted ? 'refused' : 'accepted'}`).to.be.lessThan(10);
        return expectConfiguredRoot(accepted, attempt + 1);
    });

describe('VFS mount point allowed root schemes', () => {
    before(function () {
        clearAllowedSchemes();
    });

    beforeEach(function () {
        cy.executeGroovy('cleanup.groovy');
        cy.executeGroovy('createDir.groovy');
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

    it('refuses the scheme again once the configuration stops naming it', function () {
        setAllowedSchemes(CONFIGURED_SCHEMES);
        expectConfiguredRoot(true);

        clearAllowedSchemes();

        expectConfiguredRoot(false);
    });
});
