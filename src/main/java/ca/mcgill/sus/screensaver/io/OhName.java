package ca.mcgill.sus.screensaver.io;

public class OhName {
	public String short_user, first_name, last_name;

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("OhName [shortUser=").append(short_user).append(", firstName=").append(first_name)
				.append(", lastName=").append(last_name).append("]");
		return builder.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((first_name == null) ? 0 : first_name.hashCode());
		result = prime * result + ((last_name == null) ? 0 : last_name.hashCode());
		result = prime * result + ((short_user == null) ? 0 : short_user.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		OhName other = (OhName) obj;
		if (first_name == null) {
			if (other.first_name != null)
				return false;
		} else if (!first_name.equals(other.first_name))
			return false;
		if (last_name == null) {
			if (other.last_name != null)
				return false;
		} else if (!last_name.equals(other.last_name))
			return false;
		if (short_user == null) {
			if (other.short_user != null)
				return false;
		} else if (!short_user.equals(other.short_user))
			return false;
		return true;
	}
	
	
}
