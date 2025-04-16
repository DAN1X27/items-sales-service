package danix.app.users_service.mapper;

import danix.app.users_service.dto.ResponseCommentDTO;
import danix.app.users_service.models.Comment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface CommentMapper {

	@Mapping(target = "senderId", source = "owner.id")
	ResponseCommentDTO toResponseCommentDTO(Comment comment);

	List<ResponseCommentDTO> toResponseCommentDTOList(List<Comment> comment);

}
