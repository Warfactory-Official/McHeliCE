#version 330 core

layout(location = 0) in vec3 aPos;
layout(location = 1) in vec3 aTexCoord;
layout(location = 2) in vec3 aNormal;

out vec3 TexCoord;
out vec3 VertexColor;

uniform mat4 model;
uniform mat4 view;
uniform mat4 projection;
uniform mat3 normalMatrix;

uniform vec3 lightDir;
uniform vec3 lightColor;
uniform vec3 ambientColor;
uniform vec3 viewPos;
uniform vec3 materialDiffuse;
uniform vec3 materialSpecular;
uniform float shininess;

void main() {
    vec3 fragPos = vec3(model * vec4(aPos, 1.0));
    vec3 normal = normalize(normalMatrix * aNormal);

    // Lighting directions
    vec3 L = normalize(-lightDir);
    vec3 V = normalize(viewPos - fragPos);
    vec3 H = normalize(L + V);

    // Phong terms – fixed pipeline default apparently
    float diff = max(dot(normal, L), 0.0);
    float spec = pow(max(dot(normal, H), 0.0), shininess);

    vec3 ambient = ambientColor * materialDiffuse;
    vec3 diffuse = diff * lightColor * materialDiffuse;
    vec3 specular = spec * lightColor * materialSpecular;

    VertexColor = ambient + diffuse + specular;

    TexCoord = aTexCoord;
    gl_Position = projection * view * vec4(fragPos, 1.0);
}