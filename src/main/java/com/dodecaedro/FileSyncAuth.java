package com.dodecaedro;

import io.javalin.Context;
import io.javalin.Handler;
import io.javalin.security.AccessManager;
import io.javalin.security.Role;

import java.util.Set;

public class FileSyncAuth implements AccessManager {
	private final FileSyncProperties properties;

	public FileSyncAuth(FileSyncProperties properties) {
		this.properties = properties;
	}

	@Override
	public void manage(Handler handler, Context context, Set<Role> permittedRoles) throws Exception {
		if (isNotValid(context.queryParam("key"))) {
			context.status(401).json("Unauthorized");
		} else {
			handler.handle(context);
		}
	}

	private boolean isNotValid(String key) {
		return !properties.getKey().equals(key);
	}
}
