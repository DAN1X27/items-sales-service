package danix.app.authentication_service.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.io.Serializable;
import java.util.Date;

@Table("tokens")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Token implements Persistable<String>, Serializable {

	@Id
	private String id;

	@Column("user_id")
	private Long userId;

	@Column("expired_date")
	private Date expiredDate;

	@Override
	public boolean isNew() {
		return true;
	}
}