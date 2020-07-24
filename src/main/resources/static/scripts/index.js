$("#uploadFileInFolder").click(function(){
		$.ajax({
			url: '/uploadinfolder'
		}).done(function(data){
			alert(data.fileID);
		});
	});