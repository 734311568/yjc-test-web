
function myclike() {
	//初始化模态框
	$("#mymodal").modal({
		//点击背景不关闭
		backdrop: "static",
		//触发键盘esc事件时不关闭
		keyboard: false
	});
	$('#mymodel').modal('show');
	//设置模态框隐藏事件
	$('#mymodal').on('hide.bs.modal', function () {
		//	alert("我被隐藏");
		var textValue = $("input#foo").val();
		//实例化插件 但返回的值
		var clipboard = new ClipboardJS('.btn', {
			text: function () {
				return textValue;
			}});

		clipboard.on('success', function (e) {//复制成功时候
			//alert("复制" + e.text);
			console.info('Action:', e.action); //触发复制行为
			console.info('Text:', e.text);
			console.info('Trigger:', e.trigger);
			e.clearSelection();
		});
		clipboard.on('error', function (e) {//复制失败
			console.error('Action:', e.action);
			console.error('Trigger:', e.trigger);
			alert('请选择“拷贝”进行复制!');
		});
              
	});
}
