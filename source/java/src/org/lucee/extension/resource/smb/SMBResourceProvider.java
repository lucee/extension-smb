/**
 *
 * Copyright (c) 2014, the Railo Company Ltd. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either 
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public 
 * License along with this library.  If not, see <http://www.gnu.org/licenses/>.
 * 
 **/
package org.lucee.extension.resource.smb;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.charset.Charset;
import java.util.Base64;
import java.util.Map;
import java.util.Properties;

import lucee.loader.engine.CFMLEngineFactory;

import jcifs.CIFSContext;
import jcifs.CIFSException;
import jcifs.SmbResource;
import jcifs.context.BaseContext;
import jcifs.config.PropertyConfiguration;
import jcifs.smb.NtlmPasswordAuthenticator;
import jcifs.smb.SmbFile;
import lucee.commons.io.res.Resource;
import lucee.commons.io.res.ResourceLock;
import lucee.commons.io.res.ResourceProvider;
import lucee.commons.io.res.Resources;

public class SMBResourceProvider implements ResourceProvider {

	private String scheme = "smb";
	private Map<String, String> args;
	private final static String ENCRYPTED_PREFIX = "$smb-enc$";
	private final static Charset UTF8 = Charset.forName("UTF-8");
	private int lockTimeout = 20000;
	private ResourceLock lock;
	private CIFSContext baseContext;

	@Override
	public ResourceProvider init(String scheme, Map arguments) {
		if (!CFMLEngineFactory.getInstance().getStringUtil().isEmpty(scheme)) this.scheme = scheme;
		this.args = arguments;

		// Initialize CIFSContext with properties
		try {
			baseContext = _createContext(arguments);
		}
		catch (CIFSException e) {
			throw new RuntimeException("Failed to initialize CIFS context", e);
		}

		// lock-timeout
		if (arguments != null) {
			String strTimeout = (String) arguments.get("lock-timeout");
			if (strTimeout != null) {
				try {
					lockTimeout = Integer.parseInt(strTimeout);
				}
				catch (Exception e) {
				}
			}
		}

		return this;
	}

	private CIFSContext _createContext(Map arguments) throws CIFSException {
		Properties props = new Properties();

		// Set default properties
		props.setProperty("jcifs.resolveOrder", "DNS");
		props.setProperty("jcifs.smb.client.dfs.disabled", "true");

		// Override with provided arguments
		if (arguments != null) {
			// Name resolution
			String resolveOrder = (String) arguments.get("resolveOrder");
			if (resolveOrder != null) props.setProperty("jcifs.resolveOrder", resolveOrder);

			// DFS
			String dfsDisabled = (String) arguments.get("dfsDisabled");
			if (dfsDisabled != null) props.setProperty("jcifs.smb.client.dfs.disabled", dfsDisabled);

			// Protocol versions (SMB1, SMB202, SMB210, SMB300, SMB302, SMB311)
			String minVersion = (String) arguments.get("minVersion");
			if (minVersion != null) props.setProperty("jcifs.client.minVersion", minVersion);

			String maxVersion = (String) arguments.get("maxVersion");
			if (maxVersion != null) props.setProperty("jcifs.client.maxVersion", maxVersion);

			// Timeouts (in milliseconds)
			String soTimeout = (String) arguments.get("soTimeout");
			if (soTimeout != null) props.setProperty("jcifs.client.soTimeout", soTimeout);

			String connTimeout = (String) arguments.get("connTimeout");
			if (connTimeout != null) props.setProperty("jcifs.client.connTimeout", connTimeout);

			String responseTimeout = (String) arguments.get("responseTimeout");
			if (responseTimeout != null) props.setProperty("jcifs.client.responseTimeout", responseTimeout);

			// Signing
			String signingPreferred = (String) arguments.get("signingPreferred");
			if (signingPreferred != null) props.setProperty("jcifs.client.signingPreferred", signingPreferred);

			String signingEnforced = (String) arguments.get("signingEnforced");
			if (signingEnforced != null) props.setProperty("jcifs.client.signingEnforced", signingEnforced);

			// SMB3 encryption
			String encryptionEnabled = (String) arguments.get("encryptionEnabled");
			if (encryptionEnabled != null) props.setProperty("jcifs.client.encryptionEnabled", encryptionEnabled);

			// Custom port (for testing)
			String port = (String) arguments.get("port");
			if (port != null) props.setProperty("jcifs.smb.client.port", port);
		}

		// Also check system properties for port (for testing)
		String sysPort = System.getProperty("jcifs.smb.client.port");
		if (sysPort != null) props.setProperty("jcifs.smb.client.port", sysPort);

		return new BaseContext(new PropertyConfiguration(props));
	}

	public Resource getResource(String path, NtlmPasswordAuthenticator auth) {
		return new SMBResource(CFMLEngineFactory.getInstance(), this, path, auth);
	}

	@Override
	public Resource getResource(String path) {
		return new SMBResource(CFMLEngineFactory.getInstance(), this, path);
	}

	@Override
	public String getScheme() {
		return scheme;
	}

	@Override
	public Map<String, String> getArguments() {
		return args;
	}

	@Override
	public void setResources(Resources resources) {
		lock = resources.createResourceLock(lockTimeout, false);
	}

	@Override
	public void unlock(Resource res) {
		lock.unlock(res);
	}

	@Override
	public void lock(Resource res) throws IOException {
		lock.lock(res);
	}

	@Override
	public void read(Resource res) throws IOException {
		lock.read(res);
	}

	@Override
	public boolean isCaseSensitive() {
		return false;
	}

	@Override
	public boolean isModeSupported() {
		return false;
	}

	@Override
	public boolean isAttributesSupported() {
		return true;
	}

	public SmbResource getFile(String path, NtlmPasswordAuthenticator auth) {
		try {
			CIFSContext ctx = baseContext;
			if (auth != null) {
				ctx = baseContext.withCredentials(auth);
			}
			return new SmbFile(path, ctx);
		}
		catch (MalformedURLException e) {
			return null; // null means it is a bad SMBFile
		}
	}

	public CIFSContext getContext() {
		return baseContext;
	}

	public static boolean isEncryptedUserInfo(String userInfo) {
		if (userInfo == null) return false;
		return userInfo.startsWith(ENCRYPTED_PREFIX);
	}

	public static String unencryptUserInfo(String userInfo) {
		if (userInfo == null) return null;
		if (!isEncryptedUserInfo(userInfo)) return userInfo;
		String encrypted = userInfo.substring(ENCRYPTED_PREFIX.length());
		byte[] unencryptedBytes = Base64.getUrlDecoder().decode(encrypted);
		return new String(unencryptedBytes, UTF8);
	}

	public static String encryptUserInfo(String userInfo) {
		if (userInfo == null) return null;
		String encoded = Base64.getUrlEncoder().withoutPadding().encodeToString(userInfo.getBytes(UTF8));
		return ENCRYPTED_PREFIX.concat(encoded);
	}
}
