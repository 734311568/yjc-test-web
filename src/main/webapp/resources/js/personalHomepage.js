



function myclike() {
	var btns = new Array();
	var img = new Array();
	$("a.ioc").each(function (i) {
		btns[i] = $(this).attr("valueId");
	//	alert(btns[i]);
		$.get("/img", {"id": btns[i]}, function (data) {
			$('img.ioc').each(function (j) {
				img[j] = $(this).attr("imgvalueId");
			//	alert("进来图片了吗");
			//	alert(img[j]);
				//对比如果留言者id的一样就给对应的属性赋值
				if (btns[i] === img[j]) {
					$(this).attr("src", data);
				}
			});

		}, 'text');
	});
}




