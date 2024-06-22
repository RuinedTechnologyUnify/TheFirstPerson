#version 150

#moj_import <light.glsl>
#moj_import <fog.glsl>

in vec3 Position;
in vec4 Color;
in vec2 UV0;
in vec2 UV1;
in ivec2 UV2;
in vec3 Normal;

uniform sampler2D Sampler2;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
uniform int FogShape;

uniform vec3 Light0_Direction;
uniform vec3 Light1_Direction;

out float vertexDistance;
out vec4 vertexColor;
out vec2 texCoord0;
out vec2 texCoord1;
out vec2 texCoord2;

#define SPACING 1024.
#define MAXRANGE (0.5 * SPACING)

void main() {
    vertexColor = minecraft_mix_light(Light0_Direction, Light1_Direction, Normal, Color) * texelFetch(Sampler2, UV2 / 16, 0);
    texCoord0 = UV0;
    texCoord1 = UV1;
    texCoord2 = UV2;

    vec3 wpos = Position;
    int partId = -int((wpos.y - MAXRANGE) / SPACING);

    if (partId == 0) {
        gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
        vertexDistance = fog_distance(Position, FogShape);
    } else {
        // 가상 팔과 같은 효과 적용
        // @see rendertype_entity_translucent.fsh
        wpos.y += SPACING * partId;

        mat4 newProjMat = ProjMat;

        float tanFovHalf = ProjMat[1][1] / ProjMat[0][0];
        float newTanFovHalf = tan(80.1 / 2.0);
        newProjMat[0][0] /= newTanFovHalf * ProjMat[1][1];
        newProjMat[1][1] = newTanFovHalf;

        gl_Position = vec4((newProjMat  * vec4(wpos.x, -wpos.y, -wpos.z, 1.0)).xyz, 0.5);
        vertexDistance = fog_distance(wpos, FogShape);
    }
}