angular.module('orcidApp').controller('widgetCtrl',['$scope', function ($scope){
    $scope.hash = orcidVar.orcidIdHash.substr(0, 6);
    $scope.showCode = false;

    $scope.widgetURLND = '<a href="'+ getBaseUri() + '/' + orcidVar.orcidId + '" target="_blank" rel="noopener noreferrer" style="vertical-align:top;"><img src="https://orcid.org/sites/default/files/images/orcid_16x16.png" style="width:1em;margin-right:.5em;" alt="ORCID iD icon">' + getBaseUri() + '/' + orcidVar.orcidId + '</a>';
    $scope.inputTextAreaSelectAll = function($event){
        $event.target.select();
    }
    
    $scope.toggleCopyWidget = function(){
        $scope.showCode = !$scope.showCode;
    }
    
    $scope.hideWidgetCode = function(){
        $scope.showCode = false;
    }
    
}]);