import {addNode, deleteNode, getNodeByPath} from '@jahia/cypress';
import {publishAndWaitJobEnding} from '@jahia/cypress';

describe('Tests for the identifier cache', () => {
    const mountInfo = {name: 'my-vfs-mount', rootPath: '/tmp/mount-test'};
    const pageName = 'identifier-cache-test';
    const expectedExternalMappingEntriesCount = 3; // 3 entries: '/', '/index.html', '/index.html/jcr:content'

    before(function () {
        deleteAllExternalMappingEntries();
        shouldExternalMappingEntriesCountBe(0, 'Should be empty after flushing it'); // Sanity check
        cy.executeGroovy('cleanup.groovy');
        cy.executeGroovy('createDir.groovy');
        mountVfsFolder();
    });

    after(function () {
        cy.executeGroovy('cleanup.groovy');
        deleteNode('/sites/digitall/' + pageName);
    });

    it('Should use cache even if the external mapping entries table is empty once the cache is populated', () => {
        getNodeByPath('/sites/digitall/files/my-vfs-mount/index.html').then(response => {
            return response.data.jcr.nodeByPath.uuid as string;
        }).then(indexHtmlUuid => {
            createPageWithFileReference(indexHtmlUuid);
            shouldExternalMappingEntriesCountBe(expectedExternalMappingEntriesCount, 'Should contain entries after creating the page');

            // First visit
            visitPage();
            shouldExternalMappingEntriesCountBe(expectedExternalMappingEntriesCount, 'Should still contain the same number of entries');

            // Flush the entries that have been cached
            deleteAllExternalMappingEntries();

            // Second visit
            visitPage();
            shouldExternalMappingEntriesCountBe(0, 'Should remain empty as the cache should have been used');
        });
    });

    /**
     * Mount the VFS folder /tmp/mount-test under /sites/digitall/files/my-vfs-mount
     */
    function mountVfsFolder() {
        cy.apollo({
            mutationFile: 'mountVfs.graphql',
            variables: {...mountInfo, mountPointRefPath: '/sites/digitall/files'}
        }).then(resp => {
            expect(resp.data.admin.mountPoint.addVfs).to.not.be.empty;
        });
    }

    /**
     * Create a page with a file reference to index.html and publish it
     */
    function createPageWithFileReference(uuid: string) {
        return deleteNode('/sites/digitall/' + pageName).then(() => {
            return addNode({
                parentPathOrId: '/sites/digitall',
                name: pageName,
                primaryNodeType: 'jnt:page',
                properties: [
                    {name: 'jcr:title', value: pageName, language: 'en'},
                    {name: 'j:templateName', value: 'simple'}
                ],
                children: [
                    {
                        name: 'area-main',
                        primaryNodeType: 'jnt:contentList',
                        children: [
                            {
                                name: 'myFileRef',
                                primaryNodeType: 'jnt:fileReference',
                                properties: [
                                    {name: 'j:node', value: uuid}
                                ]
                            }
                        ]
                    }
                ]
            });
        }).then(() => {
            // Publish and wait for the job to complete
            publishAndWaitJobEnding('/sites/digitall/' + pageName);
        });
    }

    function shouldExternalMappingEntriesCountBe(count: number, msg:string) {
        cy.executeGroovy('shouldExternalMappingEntriesCountMatch.groovy', {
            EXPECTED_COUNT: String(count)
        }).then(response => {
            // To check that the count matches, ensure the script executes successfully.
            // in case of a mismatch with what is expected, an exception is thrown and the script returns ".failed"
            expect(response).to.equal('.installed', msg);
        });
    }

    /**
     * Visit the page and verify the file reference is rendered
     */
    function visitPage() {
        cy.visit('/sites/digitall/' + pageName + '.html');
        cy.get('a[title="index.html"]').should('exist');
    }

    function deleteAllExternalMappingEntries() {
        cy.executeGroovy('deleteExternalMappingEntries.groovy');
    }
});
