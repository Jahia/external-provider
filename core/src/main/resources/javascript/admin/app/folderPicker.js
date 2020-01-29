angular.module('folderPicker', [])
    .controller('folderPickerCtrl', ['$scope', function ($scope) {
        $scope.tree = [];
        $scope.selectedPath = '';
        $scope.propName = '';
        $scope.selectMessage = '';
        $scope.hideAndCheckbox = false;
        $scope.enablePicker = false;

        $scope.getIcon = function (item) {
            var nodetype = item.nodetype;
            if (nodetype == 'root') {
                return 'icon-home';
            }

            if (nodetype == 'site') {
                return 'icon-globe';
            }

            if (nodetype == 'folder') {
                if (item.displayed && item.children && item.children.length > 0) {
                    return 'icon-folder-open';
                }

                return 'icon-folder-close';
            }
        };

        $scope.select = function (item) {
            return $scope.selectedPath = item.path;
        };

        $scope.isFolder = function (item) {
            return item.nodetype == 'folder';
        };

        $scope.transformFlatListToNodeTree = function (flatList) {
            var output = [];
            if (flatList) {
                for (var i = 0; i < flatList.length; i++) {
                    var chain = flatList[i].substring(1).split('/');
                    var currentNode = output;
                    for (var j = 0; j < chain.length; j++) {
                        var wantedNode = chain[j];
                        var lastNode = currentNode;
                        for (var k = 0; k < currentNode.length; k++) {
                            if (currentNode[k].name == wantedNode) {
                                currentNode = currentNode[k].children;
                                break;
                            }
                        }

                        // If we couldn't find an item in this list of children
                        // that has the right name, create one:
                        if (lastNode == currentNode) {
                            // Reconstruct path for the current node
                            var currentPath = wantedNode;
                            for (var l = j; l > 0; l--) {
                                currentPath = (chain[l - 1] + '/' + currentPath);
                            }

                            currentPath = '/' + currentPath;
                            var displayed = j < 2 ? true : ($scope.selectedPath && new RegExp('^' + currentPath).test($scope.selectedPath));

                            // Construct node
                            var newNode = currentNode[k] = {
                                name: wantedNode,
                                path: (currentPath),
                                displayed: displayed, // 2 is the start of the folders node
                                nodetype: j == 0 ? 'root' : (j == 1 ? 'site' : 'folder'), // 2 is the start of the folders node
                                children: []};
                            currentNode = newNode.children;
                        }
                    }
                }
            }

            return output;
        };

        $scope.switchPicker = function () {
            $scope.enablePicker = !$scope.enablePicker;
        };

        $scope.init = function (data, previousSelectedPath, propName, hideAndCheckbox, selectMessage) {
            $scope.propName = propName;
            $scope.selectMessage = selectMessage;
            // Transform flat folders to node tree
            $scope.hideAndCheckbox = hideAndCheckbox;
            if (!hideAndCheckbox || (hideAndCheckbox && previousSelectedPath)) {
                $scope.enablePicker = true;
            }

            if (previousSelectedPath) {
                $scope.selectedPath = previousSelectedPath;
            }

            var nodeTree = $scope.transformFlatListToNodeTree(data.folders);
            if (nodeTree[0]) {
                $scope.tree = nodeTree[0];
                $scope.tree.displayed = true;
            }
        };
    }]);
