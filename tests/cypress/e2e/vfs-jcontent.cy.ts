import {JContent as JContentCE} from '@jahia/content-editor-cypress/dist/page-object/jcontent';
import {JContent} from '@jahia/jcontent-cypress/dist/page-object/jcontent';

describe('VFS local mount tests', () => {
    beforeEach(function () {
        cy.executeGroovy('cleanup.groovy');
        cy.executeGroovy('createDir.groovy');
        cy.apollo({queryFile: 'mountLocal.graphql'});
    });

    afterEach(function () {
        cy.executeGroovy('cleanup.groovy');
    });

    it('should see the file in jcontent', function () {
        cy.login();
        const jc = JContent.visit('digitall', 'en', 'media/files');
        jc.switchToListMode();
        jc.getTable().get().contains('mount-test').dblclick();
        jc.getTable().get().contains('image').dblclick();
        jc.getTable().get().contains('tomcat.gif');
    });

    it('should be able to create a ref', function () {
        cy.login();
        const jc = JContentCE.visit('digitall', 'en', 'content-folders/contents');
        const ce = jc.createContent('File reference');
        const picker = ce.getPickerField('jnt:fileReference_j:node', false).open();
        picker.switchViewMode('List');
        picker.getTable().getRowByLabel('mount-test').dblclick();
        picker.getTable().getRowByLabel('images').dblclick();
        picker.getTable().getRowByLabel('tomcat.gif').click();
        picker.select();
        ce.save();
    });
});
