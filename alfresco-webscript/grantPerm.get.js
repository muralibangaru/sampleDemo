var nodeRef = args.noderef
var user = args.user;
var node = search.findNode(nodeRef);
try {
	node.setPermission("Read", user);
	status.message = "Permission set successfully"
	status.code = 200;
	
} catch (e) {
	// todo: handle exception
	status.message = e.message;
	status.code = 500;
}

status.redirect = true; 
