<input name="{{propName}}" type="hidden" value="" ng-disabled="enablePicker"/>

<script type="text/ng-template" id="treeItem.html">
    <input name="{{propName}}" type="radio" ng-if="isFolder(item)" ng-model="selectedPath" ng-value="item.path" ng-disabled="!enablePicker" />&nbsp;

    <span ng-click="item.displayed = !item.displayed"><i ng-class="getIcon(item)"></i> {{item.name}}</span>

    <ul ng-show="item.displayed">
        <li ng-repeat="item in item.children" ng-include="'treeItem.html'" class="parent_li">
        </li>
    </ul>
</script>

<label ng-if="hideAndCheckbox">
    <input type="checkbox" ng-model="enablePicker" ng-click="switchPicker()"/>&nbsp;{{selectMessage}}
</label>

<div ng-show="enablePicker">
    <div class="tree well">
        <ul>
            <li class="parent_li">
                <span ng-click="tree.displayed = !tree.displayed"><i ng-class="getIcon(tree)"></i>{{tree.name}}</span>
                <ul ng-show="tree.displayed">
                    <li ng-repeat="item in tree.children" ng-include="'treeItem.html'"
                        class="parent_li">

                    </li>
                </ul>
            </li>
        </ul>
    </div>
</div>
