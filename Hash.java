import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Helper class to provide for hashing Strings with SHA-1
 * and returning the hash as a String
 * @author Kat Winter
 */
public abstract class Hash {

	public static String hash(String toBeHashed) {
		
		String hashed = "";
		
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-1");
			hashed = byteArrayToHexString(digest.digest(toBeHashed.getBytes("UTF-8")));
		} catch (NoSuchAlgorithmException e) {
			System.out.println("SHA-1 not found as hashing type");
		} catch (UnsupportedEncodingException e) {
			System.out.println("Encoding using UTF-8 is not supported");
		}
		
		return hashed;
	}
	
	private static String byteArrayToHexString(byte[] bytes) {
		
		String result = "";
		
		for (int i = 0; i < bytes.length; i++) {
			
			result += Integer.toString( (bytes[i] & 0xff) + 0x100, 16).substring(1);
		}
		
		return result;
	}
}
