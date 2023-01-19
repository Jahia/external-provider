# Not Very Obvious Test Cases

These are some test cases to watch out for. They are not very obvious but were a source of issues and need to be tested for.

###Case 1 (can switch between empty pickers of different type)

1. Start new content creation and have two pickers (site, editorial)
2. Open site picker, sites should be displayed, close without saving
3. Open editorial, specified content should be displayed (e. g. news), close without saving
4. Open site picker again, sites should be displayed

###Case 2 (can switch between pickers of different type with one selection)

1. Edit content and have two pickers (site, editorial), site should have a selection.
2. Open site picker, sites should be displayed and selection selected, close without saving
3. Open editorial, specified content should be displayed (e. g. news), close without saving
4. Open site picker again, sites should be displayed along with selected selection

###Case 3 (can switch between pickers of different type with two selection)

1. Edit content and have two pickers (site, editorial), both should have a selection.
2. Open site picker, sites should be displayed and selection selected, close without saving
3. Open editorial, specified content should be displayed (e. g. news) and selection selected, close without saving
4. Open site picker again, sites should be displayed along with selected selection

###Case 4 (remember previous location in the same mode)

1. Edit content with image picker, it should have a selection
2. Open picker, selections should be selected, close picker
3. Create new content with image picker
4. Open picker, the media accordion should open the same path as previous picker, nothing should be selected
5. Edit content with editorial picker
6. Open picker, default path (home) should be selected in the pages accordion, content visible in table, nothing selected

###Case 5 (can switch to different site with empty picker for context site)

1. Edit content with any editorial picker
2. Open picker, content should be visible, nothing selected, close picker
3. Edit content with editorial picker referencing a different site
4. Open picker, content should be selected, site should be switched, close picker
5. Edit content from step 1
6. Open picker, content should be visible, nothing selected, site switched

###Case 6 (can switch to different site with selection for context site)

1. Edit content with any editorial picker and existing selection
2. Open picker, content should be selected, close picker
3. Edit content with editorial picker referencing a different site
4. Open picker, content should be selected, site should be switched, close picker
5. Edit content from step 1 (or any other in current site)
6. Open picker, content should be selected site switched


