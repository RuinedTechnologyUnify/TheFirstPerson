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

// 원본의 설명에서는, item_display의 transformation.transition 의 값을 1024*n으로 설정해 파트를 구별한다고 한다. 해당 값으로 보인다.
#define SPACING 1024.
// 이 또한 원본의 설명에서 vertical range를 1024/2 로 제한한다 하는데, 해당 값으로 보인다.
#define MAXRANGE (0.5 * SPACING)
#define SKINRES 64
#define FACERES 8

// uv 크기 데이터로 보인다.
const vec4[] subuvs = vec4[](
vec4(4.0, 0.0, 8.0, 4.0), // 4x4x12
vec4(8.0, 0.0, 12.0, 4.0),
vec4(0.0, 4.0, 4.0, 16.0),
vec4(4.0, 4.0, 8.0, 16.0),
vec4(8.0, 4.0, 12.0, 16.0),
vec4(12.0, 4.0, 16.0, 16.0),
vec4(4.0, 0.0, 7.0, 4.0), // 4x3x12
vec4(7.0, 0.0, 10.0, 4.0),
vec4(0.0, 4.0, 4.0, 16.0),
vec4(4.0, 4.0, 7.0, 16.0),
vec4(7.0, 4.0, 11.0, 16.0),
vec4(11.0, 4.0, 14.0, 16.0),
vec4(4.0, 0.0, 12.0, 4.0), // 4x8x12
vec4(12.0, 0.0, 20.0, 4.0),
vec4(0.0, 4.0, 4.0, 16.0),
vec4(4.0, 4.0, 12.0, 16.0),
vec4(12.0, 4.0, 16.0, 16.0),
vec4(16.0, 4.0, 24.0, 16.0)
);

// uv 위치 데이터로 보인다.
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

// faceId에서 사용하는데, 명확하게 모르겠다.
// 작성 시점에 right_arm의 아래 부분에 이상한 텍스쳐가 겹치는 문제가 있는데, 이 변수에 영향을 받음이 확인되었다.
const int[] faceremap = int[](0, 0, 1, 1, 2, 3, 4, 5);

void main() {
    // 바닐라 로직. texCoord0과 gl_Position은 추후 설정하는 듯
    vertexColor = minecraft_mix_light(Light0_Direction, Light1_Direction, Normal, Color);
    lightMapColor = texelFetch(Sampler2, UV2 / 16, 0);
    overlayColor = texelFetch(Sampler1, UV1, 0);

    // Sampler0은 원래 frag shader에만 들어가는 값... 스킨 정보일 것으로 보인다.
    ivec2 dim = textureSize(Sampler0, 0);

    if (ProjMat[2][3] == 0. || dim.x != 64 || dim.y != 64) {
        // 플레이어 머리가 아닌 경우
        // ProjMat 관련은 보다 연구가 필요
        part = 0.;
        texCoord0 = UV0;
        texCoord1 = vec2(0.0);
        vertexDistance = fog_distance(Position, FogShape);
        gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
    } else {
        // wpos는 원래 uniform mat3 IViewRotMat을 곱해야 하나, 1.20.4 이후 deprecated 되었다.
        // McTsts/Minecraft-Shaders-Wiki에 따르면 ModelViewMat의 역행렬이다.
        // 업데이트 로그에 의하면 Position에게도 변경점이 있다. Position 자체가 World Space라고 하는데...
        // 결론! Entity Shader에선 ModelViewMat 자체가 항등행렬이다. 이유는 몰라도 World Space를 View Space로 변환하는 과정이 불필요한 듯 하다.
        vec3 wpos = Position;
        // 핵심 로직으로 보인다. 이 둘에 조작을 가해 출력하는 것 같다.
        vec2 UVout = UV0;
        vec2 UVout2 = vec2(0.);
        // 원본에서는 head, arm_r, arm_l, torso, leg_r, leg_l 순으로 translation이 0부터 1024씩 줄어든다.
        // 따라서 각각 0, 1, 2, 3, 4, 5 + 1/2 의 int cast가 들어올 것이다. 1/2을 굳이 더하는 이유는 부동소수점 이슈로 보인다.
        int partId = -int((wpos.y - MAXRANGE) / SPACING);

        part = float(partId);

        // 파트 별로 렌더링하는 부분
        if (partId == 0) {
            gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
        } else {
            // 아래 것이 1픽셀 더 오른쪽 픽셀을 요구하는데, 오직 slim 감지에만 사용된다.
            vec4 samp1 = texture(Sampler0, vec2(54.0 / 64.0, 20.0 / 64.0));
            vec4 samp2 = texture(Sampler0, vec2(55.0 / 64.0, 20.0 / 64.0));
            bool slim = samp1.a == 0. || (((samp1.r + samp1.g + samp1.b) == 0.0) && ((samp2.r + samp2.g + samp2.b) == 0.0) && samp1.a == 1.0 && samp2.a == 1.);
            // 아마, 바깥이면 1 안쪽이면 0?
            int outerLayer = (gl_VertexID / 24 ) % 2;
            // 한 face에서 어느 꼭짓점인지 결정
            int vertexId = gl_VertexID % 4;
            // 한 part에서 어느 face인지 결정 : 0부터 4개씩 끊어 up down right front left back 순서
            int faceId = (gl_VertexID / 4) % 6;

            // faceId에 따라 uv 위치 조정
            int subuvIndex = faceId;

            // origin을 모델 위치로 변경
            wpos.y += SPACING * partId;

            // ProjMat 조작 작업
            // TODO nausea 포션효과 무시 작업
            // TODO 완벽한 FOV 찾기(80.1은 실험적 값)
            mat4 newProjMat = ProjMat;

            // FOV에 따른 ProjMat 변형을 조작 -> FOV 무시하고 위치 고정
            float tanFovHalf = ProjMat[1][1] / ProjMat[0][0];
            float newTanFovHalf = tan(80.1 / 2.0);

            newProjMat[0][0] /= newTanFovHalf * ProjMat[1][1];
            newProjMat[1][1] = newTanFovHalf;
//            newProjMat[3][0] = 0; 위아래 bobbing만 적용
//            newProjMat[3][1] = 0; 좌우 bobbing만 적용

            // ModelViewMat 삭제하여 billboard:"center" 효과 적용
            gl_Position = vec4((newProjMat * vec4(wpos.x, wpos.y, wpos.z, 2.0)).xyz, 4);


            // uv 매핑
            UVout = origins[2 * (partId - 1) + outerLayer];
            UVout2 = origins[2 * (partId - 1)];

            // 각 vertex별 uv 할당
            vec2 offset = vec2(0.);
            if (faceId == 0) {
                offset += vec2(4,4);
                if (vertexId == 0) {
                    offset += vec2(4,0);
                } else if (vertexId == 1) {
                    offset += vec2(0);
                } else if (vertexId == 2) {
                    offset += vec2(0,-4);
                } else if (vertexId == 3) {
                    offset += vec2(4,-4);
                }
            } else if (faceId == 1) {
                offset += vec2(8,4);
                if (vertexId == 0) {
                    offset += vec2(4,0);
                } else if (vertexId == 1) {
                    offset += vec2(0);
                } else if (vertexId == 2) {
                    offset += vec2(0,-4);
                } else if (vertexId == 3) {
                    offset += vec2(4,-4);
                }
            } else {
                offset += vec2((faceId-2)*4, 4);
                if (vertexId == 0) {
                    offset += vec2(4,0);
                } else if (vertexId == 1) {
                    offset += vec2(0);
                } else if (vertexId == 2) {
                    offset += vec2(0, 12);
                } else if (vertexId == 3) {
                    offset += vec2(4, 12);
                }
            }

            vertexDistance = fog_distance(Position, FogShape);
            UVout += offset;
            UVout2 += offset;
            UVout /= float(SKINRES);
            UVout2 /= float(SKINRES);
        }

        texCoord0 = UVout;
        texCoord1 = UVout2;
    }


}