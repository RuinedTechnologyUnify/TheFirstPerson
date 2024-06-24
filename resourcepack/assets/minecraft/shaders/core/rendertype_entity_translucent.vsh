/**
* Written by lanthanide
* stable_player_display 레포의 코드를 재구현
*/
#version 150

#moj_import <light.glsl>
#moj_import <fog.glsl>

in vec3 Position;
in vec4 Color;
in vec2 UV0;
in ivec2 UV1;
in ivec2 UV2;
in vec3 Normal;

uniform sampler2D Sampler0;
uniform sampler2D Sampler1;
uniform sampler2D Sampler2;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
uniform int FogShape;
uniform float FogStart;

uniform vec3 Light0_Direction;
uniform vec3 Light1_Direction;

out float vertexDistance;
out vec4 vertexColor;
out vec4 lightMapColor;
out vec4 overlayColor;
out vec2 texCoord0;
out vec2 texCoord1;
// 어느 부분을 렌더링하는지 전달
out float part;

// item_display의 transformation.transition 의 값을 1024*n으로 설정해 파트를 구별
#define SPACING 1024.
// vertical range를 1024/2 로 제한
#define MAXRANGE (0.5 * SPACING)
#define SKINRES 64
#define FACERES 8


// uv 위치 데이터
const vec2[] origins = vec2[](
vec2(40.0, 16.0), // right arm
vec2(40.0, 32.0),
vec2(32.0, 48.0), // left arm
vec2(48.0, 48.0),
vec2(16.0, 16.0), // torso
vec2(16.0, 32.0),
vec2(0.0, 16.0), // right leg
vec2(0.0, 32.0),
vec2(16.0, 48.0), // left leg
vec2(0.0, 48.0)
);

void main() {
    // 바닐라 로직. texCoord0과 gl_Position은 추후 설정
    vertexColor = minecraft_mix_light(Light0_Direction, Light1_Direction, Normal, Color);
    lightMapColor = texelFetch(Sampler2, UV2 / 16, 0);
    overlayColor = texelFetch(Sampler1, UV1, 0);

    // Sampler0은 원래 frag shader에만 들어가는 값... 스킨 정보로 보인다.
    ivec2 dim = textureSize(Sampler0, 0);

    if (ProjMat[2][3] == 0. || dim.x != 64 || dim.y != 64) {
        // 플레이어 머리가 아닌 경우
        // TODO ProjMat[2][3] 의 의미에 대해서 연구가 필요
        part = 0.;
        texCoord0 = UV0;
        texCoord1 = vec2(0.0);
        vertexDistance = fog_distance(Position, FogShape);
        gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
    } else {
        vec3 wpos = Position;
        // uv 매핑을 위한 데이터
        vec2 UVout = UV0;
        vec2 UVout2 = vec2(0.);
        // head, arm_r, arm_l, torso, leg_r, leg_l 순으로 translation이 0부터 1024씩 줄어든다.
        // 따라서 각각 0, 1, 2, 3, 4, 5 + 1/2 의 int cast가 들어올 것이다. 1/2을 굳이 더하는 이유는 부동소수점 이슈로 보인다.
        int partId = -int((wpos.y - MAXRANGE) / SPACING);

        part = float(partId);

        // 파트 별로 렌더링
        if (partId == 0) {
            // head인 경우. 추가적 작업 불필요
            gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
            vertexDistance = fog_distance(Position, FogShape);
        } else {
            // 아래 것이 1픽셀 더 오른쪽 픽셀을 요구하는데, 오직 slim 감지에만 사용된다.
            vec4 samp1 = texture(Sampler0, vec2(54.0 / 64.0, 20.0 / 64.0));
            vec4 samp2 = texture(Sampler0, vec2(55.0 / 64.0, 20.0 / 64.0));
            bool slim = samp1.a == 0. || (((samp1.r + samp1.g + samp1.b) == 0.0) && ((samp2.r + samp2.g + samp2.b) == 0.0) && samp1.a == 1.0 && samp2.a == 1.);
            // 바깥이면 1 안쪽이면 0
            int outerLayer = (gl_VertexID / 24) % 2;
            // 한 face에서 어느 꼭짓점인지 결정
            int vertexId = gl_VertexID % 4;
            // 한 part에서 어느 face인지 결정 : 0부터 4개씩 끊어 up down right front left back 순서
            int faceId = (gl_VertexID / 4) % 6;

            // origin을 모델 위치로 변경
            wpos.y += SPACING * partId;

            // ProjMat 조작 작업
            // TODO nausea 포션효과 무시 작업
            // TODO 완벽한 FOV 찾기(80.1은 실험적 값)
            mat4 tweakedProjMat = ProjMat;

            // FOV에 따른 ProjMat 변형을 조작 -> FOV 무시하고 위치 고정
            float tanFovHalf = tan(80.1 / 2.0);

            tweakedProjMat[0][0] /= tanFovHalf * ProjMat[1][1];
            tweakedProjMat[1][1] = tanFovHalf;
            tweakedProjMat[2][2] /= 10;
            //            newProjMat[3][0] = 0; 좌우 View Bobbing 제거
            //            newProjMat[3][1] = 0; 상하 Vie Bobbing 제거

            // ModelViewMat 삭제하여 billboard:"center" 효과 적용
            gl_Position = tweakedProjMat  * vec4(-wpos.x, -wpos.y, wpos.z, 2);


            // uv 매핑
            UVout = origins[2 * (partId - 1) + outerLayer];
            UVout2 = origins[2 * (partId - 1)];

            // 각 vertex별 uv 할당
            // TODO right arm 제외한 part 테스트
            vec2 offset = vec2(0.);
            if (faceId == 0) {
                offset += vec2(4, 4);
                if (vertexId == 0) {
                    offset += vec2(4, 0);
                } else if (vertexId == 1) {
                    offset += vec2(0);
                } else if (vertexId == 2) {
                    offset += vec2(0, -4);
                } else if (vertexId == 3) {
                    offset += vec2(4, -4);
                }
            } else if (faceId == 1) {
                offset += vec2(8, 4);
                if (vertexId == 0) {
                    offset += vec2(4, 0);
                } else if (vertexId == 1) {
                    offset += vec2(0);
                } else if (vertexId == 2) {
                    offset += vec2(0, -4);
                } else if (vertexId == 3) {
                    offset += vec2(4, -4);
                }
            } else {
                offset += vec2((faceId-2)*4, 4);
                if (vertexId == 0) {
                    offset += vec2(4, 0);
                } else if (vertexId == 1) {
                    offset += vec2(0);
                } else if (vertexId == 2) {
                    offset += vec2(0, 12);
                } else if (vertexId == 3) {
                    offset += vec2(4, 12);
                }
            }

            vertexDistance = fog_distance(wpos, FogShape);
            UVout += offset;
            UVout2 += offset;
            UVout /= float(SKINRES);
            UVout2 /= float(SKINRES);
        }

        texCoord0 = UVout;
        texCoord1 = UVout2;
    }


}